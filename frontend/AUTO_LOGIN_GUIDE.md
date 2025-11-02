# Google OAuth2 자동 로그인 구현 가이드

## 🔍 자동 로그인 메커니즘

사용자가 경험하는 **"랜딩 페이지 → 자동 로그인"** 현상은 주로 두 가지 방법으로 구현됩니다:

---

## 방법 1: Google One Tap + Automatic Sign-In ⭐ (가장 현대적)

### 동작 원리
```
1. 사용자가 웹사이트 접속
2. 페이지 로드 시 Google One Tap 스크립트 자동 실행
3. 사용자가 이전에 Google로 로그인한 적이 있고 동의를 제공했다면
4. → 사용자 클릭 없이 자동으로 JWT 토큰 발급
5. 프론트엔드가 토큰을 백엔드로 전송하여 세션 생성
6. 자동 로그인 완료
```

### 구현 방법 (프론트엔드)

```typescript
import { useGoogleOneTapLogin } from '@react-oauth/google';

function App() {
  useGoogleOneTapLogin({
    onSuccess: async (credentialResponse) => {
      // 1. Google에서 자동으로 JWT 토큰 받음 (사용자 클릭 불필요)
      const token = credentialResponse.credential;

      // 2. 백엔드로 토큰 전송하여 세션 생성
      const response = await fetch('/api/auth/google/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token })
      });

      const data = await response.json();
      // 3. Access Token 저장 및 사용자 정보 설정
      setAccessToken(data.accessToken);
      setUser(data.user);
      // 4. 자동 로그인 완료 → 메인 페이지로 이동
    },
    auto_select: true,  // ⭐ 핵심: 자동 선택 활성화
    cancel_on_tap_outside: false,
  });

  return <div>...</div>;
}
```

### 동작 조건
- ✅ 사용자가 Google 계정에 로그인되어 있음
- ✅ 이전에 해당 사이트에서 Google로 로그인하고 동의를 제공한 적이 있음
- ✅ `auto_select: true` 옵션 활성화

### 제약 사항
- **10분 쿨다운**: 자동 로그인 시도 사이에 10분 대기 시간 존재
- 사용자가 One Tap 팝업을 닫으면 점진적으로 쿨다운 시간 증가

---

## 방법 2: Refresh Token 기반 세션 복원 (현재 백엔드 지원)

### 동작 원리
```
1. 사용자가 웹사이트 접속
2. 프론트엔드가 Refresh Token (HttpOnly 쿠키) 존재 여부 확인
3. Refresh Token이 있으면 → /api/auth/refresh 호출
4. 백엔드가 Refresh Token 검증 후 새 Access Token 발급
5. 프론트엔드가 Access Token으로 /api/users/me 호출
6. 사용자 정보 조회 성공 → 자동 로그인 완료
```

### 구현 방법 (프론트엔드)

```typescript
// 1. 페이지 로드 시 자동 실행
useEffect(() => {
  const restoreSession = async () => {
    try {
      // 2. Refresh Token으로 새 Access Token 받기
      const response = await fetch('/api/auth/refresh', {
        method: 'POST',
        credentials: 'include', // 쿠키 자동 전송
      });

      if (response.ok) {
        const data = await response.json();
        const accessToken = data.data.accessToken;

        // 3. Access Token 저장
        setAccessToken(accessToken);

        // 4. 사용자 정보 조회
        const userResponse = await fetch('/api/users/me', {
          headers: { Authorization: `Bearer ${accessToken}` }
        });

        if (userResponse.ok) {
          const userData = await userResponse.json();
          setUser(userData);
          // 5. 자동 로그인 완료
          navigate('/dashboard');
        }
      }
    } catch (error) {
      // Refresh Token 없거나 만료됨 → 로그인 페이지 유지
      console.log('No active session');
    }
  };

  restoreSession();
}, []);
```

### 동작 조건
- ✅ Refresh Token이 HttpOnly 쿠키에 존재
- ✅ Refresh Token이 유효 (7일 이내)
- ✅ 백엔드 Redis에 Refresh Token 저장되어 있음

---

## 방법 3: 두 방법의 조합 ⭐⭐ (최적의 UX)

실제로 많은 사이트들은 **두 방법을 함께 사용**합니다:

```typescript
function App() {
  const [isCheckingSession, setIsCheckingSession] = useState(true);

  // 1단계: Refresh Token으로 세션 복원 시도
  useEffect(() => {
    const restoreSession = async () => {
      try {
        const response = await fetch('/api/auth/refresh', {
          method: 'POST',
          credentials: 'include',
        });

        if (response.ok) {
          const data = await response.json();
          setAccessToken(data.data.accessToken);

          const userResponse = await fetch('/api/users/me', {
            headers: { Authorization: `Bearer ${data.data.accessToken}` }
          });

          if (userResponse.ok) {
            const userData = await userResponse.json();
            setUser(userData);
            setIsCheckingSession(false);
            return; // 세션 복원 성공 → One Tap 불필요
          }
        }
      } catch (error) {
        console.log('No existing session');
      }

      setIsCheckingSession(false);
    };

    restoreSession();
  }, []);

  // 2단계: Refresh Token 없으면 Google One Tap 활성화
  useGoogleOneTapLogin({
    onSuccess: async (credentialResponse) => {
      // Google One Tap 자동 로그인 처리
      const token = credentialResponse.credential;

      const response = await fetch('/api/auth/google/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token })
      });

      const data = await response.json();
      setAccessToken(data.accessToken);
      setUser(data.user);
    },
    auto_select: true,
    disabled: isCheckingSession || !!user, // 세션 확인 중이거나 이미 로그인되어 있으면 비활성화
  });

  return <div>...</div>;
}
```

---

## 🔧 현재 백엔드 코드와의 통합

### 현재 백엔드 지원 상태

현재 백엔드는 **방법 2 (Refresh Token 기반)**를 완벽하게 지원합니다:
- ✅ `/api/auth/refresh` 엔드포인트 구현됨
- ✅ Refresh Token이 HttpOnly 쿠키로 전송됨
- ✅ Redis에 Refresh Token 저장 및 검증

### 방법 1 추가를 위한 백엔드 구현

**방법 1 (Google One Tap)**을 추가하려면 백엔드에 새 엔드포인트가 필요합니다:

```java
// 추가 필요한 엔드포인트
@PostMapping("/api/auth/google/verify")
public ResponseEntity<BaseResponse<TokenResponse>> verifyGoogleToken(
    @RequestBody GoogleTokenRequest request) {

    // 1. Google JWT 토큰 검증
    GoogleIdToken.Payload payload = verifyGoogleToken(request.getToken());

    // 2. 사용자 조회 또는 생성
    User user = userService.findOrCreateByEmail(payload.getEmail());

    // 3. Access Token + Refresh Token 발급
    String accessToken = jwtProvider.createAccessToken(user);
    String refreshToken = jwtProvider.createRefreshToken(user);

    // 4. Refresh Token을 Redis에 저장하고 쿠키로 전송
    refreshTokenService.storeRefreshToken(user.getId(), refreshToken);

    // 5. Refresh Token을 HttpOnly 쿠키로 설정
    ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
        .httpOnly(true)
        .secure(cookieSecure)
        .path("/")
        .maxAge(Duration.ofSeconds(refreshExpireSeconds))
        .sameSite("Lax")
        .build();
    response.addHeader("Set-Cookie", refreshCookie.toString());

    return ResponseEntity.ok(new BaseResponse<>(
        TokenResponse.of(accessToken, jwtProvider.getAccessExpireTime())
    ));
}
```

---

## 📊 비교: 방법 1 vs 방법 2

| 항목 | Google One Tap | Refresh Token |
|------|---------------|---------------|
| **사용자 경험** | ⭐⭐⭐⭐⭐ 완전 자동 | ⭐⭐⭐⭐ 거의 자동 |
| **보안** | ⭐⭐⭐⭐ Google이 관리 | ⭐⭐⭐⭐⭐ 자체 관리 |
| **구현 복잡도** | ⭐⭐⭐ 중간 | ⭐⭐ 간단 |
| **세션 기간** | Google 세션에 의존 | 7일 (설정 가능) |
| **백엔드 지원** | ❌ 추가 필요 | ✅ 구현됨 |
| **오프라인 지원** | ❌ Google 필요 | ✅ 가능 |
| **브라우저 간 공유** | ✅ Google 계정으로 공유 | ❌ 쿠키 기반 (개별) |

---

## ✅ 권장 구현 순서

### 1단계: Refresh Token 기반 세션 복원 (즉시 구현 가능)

현재 백엔드가 완전히 지원하므로 바로 구현 가능합니다.

```typescript
// src/hooks/useAuth.ts
import { useEffect } from 'react';
import { useAuthStore } from '@/stores/authStore';

export function useSessionRestore() {
  const { setAccessToken, setUser } = useAuthStore();

  useEffect(() => {
    const restoreSession = async () => {
      try {
        const response = await fetch('/api/auth/refresh', {
          method: 'POST',
          credentials: 'include',
        });

        if (response.ok) {
          const data = await response.json();
          setAccessToken(data.data.accessToken);

          const userResponse = await fetch('/api/users/me', {
            headers: { Authorization: `Bearer ${data.data.accessToken}` }
          });

          if (userResponse.ok) {
            const userData = await userResponse.json();
            setUser(userData);
          }
        }
      } catch (error) {
        console.log('No active session');
      }
    };

    restoreSession();
  }, []);
}
```

### 2단계: Google One Tap 추가 (선택 사항)

백엔드에 `/api/auth/google/verify` 엔드포인트 추가 후 구현:

```typescript
// src/App.tsx
import { useGoogleOneTapLogin } from '@react-oauth/google';
import { useSessionRestore } from '@/hooks/useAuth';

function App() {
  const { user, setAccessToken, setUser } = useAuthStore();
  const [isCheckingSession, setIsCheckingSession] = useState(true);

  // 1단계: 세션 복원
  useEffect(() => {
    restoreSession().finally(() => setIsCheckingSession(false));
  }, []);

  // 2단계: One Tap (세션 없을 때만)
  useGoogleOneTapLogin({
    onSuccess: async (credentialResponse) => {
      const response = await fetch('/api/auth/google/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: credentialResponse.credential })
      });

      const data = await response.json();
      setAccessToken(data.data.accessToken);
      setUser(data.user);
    },
    auto_select: true,
    disabled: isCheckingSession || !!user,
  });

  return <RouterProvider router={router} />;
}
```

---

## 🔒 보안 고려사항

### Refresh Token 보안
- ✅ HttpOnly 쿠키로 전송 (JavaScript 접근 불가)
- ✅ Secure 플래그 활성화 (HTTPS 필수)
- ✅ SameSite=Lax 설정 (CSRF 방지)
- ✅ Redis에 저장하여 무효화 가능

### Google One Tap 보안
- ✅ Google이 JWT 토큰 서명 검증
- ✅ 백엔드에서 Google 공개키로 토큰 재검증 필요
- ✅ 토큰 만료 시간 확인
- ✅ 이메일 도메인 제한 가능 (`hosted_domain` 옵션)

---

## 🎯 구현 체크리스트

### 필수 구현 (Refresh Token 기반)
- [ ] 페이지 로드 시 `/api/auth/refresh` 호출
- [ ] Access Token Zustand 스토어에 저장
- [ ] Access Token으로 `/api/users/me` 호출
- [ ] 세션 복원 성공 시 자동 로그인
- [ ] 세션 복원 실패 시 로그인 페이지 유지

### 선택 구현 (Google One Tap)
- [ ] 백엔드에 `/api/auth/google/verify` 엔드포인트 추가
- [ ] Google JWT 토큰 검증 로직 구현
- [ ] `@react-oauth/google` 라이브러리 설치
- [ ] `useGoogleOneTapLogin` 훅 설정
- [ ] `auto_select: true` 옵션 활성화
- [ ] 세션 확인 중일 때 One Tap 비활성화

---

## 📚 참고 자료

### Google 공식 문서
- [Google Identity Services - One Tap](https://developers.google.com/identity/gsi/web/guides/display-google-one-tap)
- [Automatic Sign-in and Sign-out](https://developers.google.com/identity/gsi/web/guides/automatic-sign-in-sign-out)
- [OAuth 2.0 for Web Server Applications](https://developers.google.com/identity/protocols/oauth2/web-server)

### React OAuth 라이브러리
- [@react-oauth/google](https://github.com/MomenSherif/react-oauth)
- [React OAuth Google Documentation](https://www.npmjs.com/package/@react-oauth/google)

### 인증 패턴
- [OAuth 2.0 Silent Authentication](https://auth0.com/docs/secure/tokens/refresh-tokens/use-refresh-tokens)
- [JWT Refresh Token Rotation](https://auth0.com/docs/secure/tokens/refresh-tokens/refresh-token-rotation)

---

## 💡 실제 구현 예시

많은 유명 서비스들이 이러한 자동 로그인을 사용합니다:

- **YouTube**: Google One Tap + Refresh Token
- **Gmail**: Google 세션 기반 자동 로그인
- **Notion**: Refresh Token 기반 세션 복원
- **Figma**: Google One Tap + 자체 세션 관리

이들은 모두 **"사용자 클릭 없이 자동으로 로그인"**되는 경험을 제공합니다! 🚀
