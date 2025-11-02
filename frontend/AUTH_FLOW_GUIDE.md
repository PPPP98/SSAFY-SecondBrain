# Google OAuth2 + JWT 인증 플로우 가이드

## 인증 플로우 개요

```
[사용자] → [Google 로그인] → [백엔드] → [프론트엔드 Callback] → [토큰 교환] → [인증 완료]
```

### 전체 플로우

1. **사용자**: "로그인" 버튼 클릭
2. **프론트엔드**: `/oauth2/authorization/google` 로 리다이렉트
3. **백엔드**: Google OAuth2 로그인 페이지로 리다이렉트
4. **사용자**: Google 계정으로 로그인 및 권한 승인
5. **Google**: 백엔드로 인증 정보 전달
6. **백엔드**:
   - 사용자 정보 저장 (신규 사용자) 또는 조회 (기존 사용자)
   - Authorization Code 생성 및 Redis 저장 (5분 TTL)
   - 프론트엔드 Callback URL로 리다이렉트 (`/auth/callback?code=xxx`)
7. **프론트엔드**:
   - URL에서 Authorization Code 추출
   - `/api/auth/token` API 호출하여 JWT 토큰으로 교환
   - Access Token을 상태 관리 (Zustand 또는 메모리)
   - Refresh Token은 자동으로 HttpOnly 쿠키에 저장됨
8. **인증 완료**: 메인 페이지로 이동

---

## 토큰 관리 전략

### Access Token
- **저장 위치**: Zustand 스토어 또는 메모리
- **유효 기간**: 1시간
- **사용 방법**: API 요청 시 `Authorization: Bearer {token}` 헤더에 포함
- **만료 시**: Refresh Token으로 자동 갱신

### Refresh Token
- **저장 위치**: HttpOnly 쿠키 (자동 저장, JavaScript 접근 불가)
- **유효 기간**: 7일
- **사용 방법**: `/api/auth/refresh` 호출 시 자동으로 쿠키가 전송됨
- **만료 시**: 재로그인 필요

---

## 프론트엔드 구현 권장 순서

### 1단계: 기본 구조 설정

#### 1-1. API 클라이언트 설정 (`api/client.ts`)
- Axios 또는 Fetch 기반 HTTP 클라이언트 생성
- Base URL 설정 (`http://localhost:8080`)
- `withCredentials: true` 설정 (쿠키 전송 허용)
- Request/Response 인터셉터 준비 (2단계에서 구현)

#### 1-2. 공통 응답 타입 정의 (`types/api.ts`)

⚠️ **중요**: 백엔드는 대부분의 API에서 `BaseResponse` 구조를 사용합니다.

```typescript
// 공통 응답 형식 (대부분의 API가 사용)
export interface BaseResponse<T> {
  success: boolean;
  code: number;
  message: string;
  data: T | null;
}
```

#### 1-3. 인증 타입 정의 (`types/auth.ts`)
```typescript
// Token 응답 데이터 (BaseResponse의 data 필드)
export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

// 사용자 정보 (GET /api/users/me는 BaseResponse 없이 직접 반환)
export interface UserInfo {
  id: number;
  email: string;
  name: string;
  picture: string;
  setAlarm: boolean;
}
```

#### 1-4. 인증 Store 생성 (`stores/authStore.ts`)
- Zustand를 사용하여 인증 상태 관리
- 상태: `accessToken`, `user`, `isAuthenticated`
- 액션: `setToken`, `setUser`, `clearAuth`

---

### 2단계: 인증 API 구현

#### 2-1. 인증 API 함수 작성 (`api/auth.ts`)

⚠️ **주의**: 인증 API는 `BaseResponse`로 감싸져 있지만, `GET /api/users/me`는 예외입니다.

```typescript
import { BaseResponse } from '@/types/api';
import { TokenResponse, UserInfo } from '@/types/auth';

// POST /api/auth/token - Authorization Code 교환
// 반환: BaseResponse<TokenResponse>
exchangeToken(code: string): Promise<BaseResponse<TokenResponse>>

// POST /api/auth/refresh - Access Token 갱신
// 반환: BaseResponse<TokenResponse>
refreshToken(): Promise<BaseResponse<TokenResponse>>

// POST /api/auth/logout - 로그아웃
// 반환: BaseResponse<null>
logout(): Promise<BaseResponse<null>>

// GET /api/users/me - 현재 사용자 정보 조회
// ⚠️ 예외: BaseResponse 없이 UserInfo 직접 반환
getCurrentUser(): Promise<UserInfo>
```

#### 2-2. Request 인터셉터 구현
- 모든 API 요청에 Access Token 자동 추가
- `Authorization: Bearer {accessToken}` 헤더 설정

#### 2-3. Response 인터셉터 구현

**BaseResponse 처리 로직**:
```typescript
axios.interceptors.response.use(
  (response) => {
    // BaseResponse 구조 확인 및 처리
    if (response.data && typeof response.data === 'object') {
      // BaseResponse인 경우
      if ('success' in response.data && 'data' in response.data) {
        // 성공 응답이면 전체 BaseResponse 반환
        if (response.data.success) {
          return response.data;
        }
        // 실패 응답이면 에러로 처리
        throw new Error(response.data.message);
      }
    }
    // GET /api/users/me는 직접 UserInfo 반환
    return response.data;
  },
  async (error) => {
    // 401 에러 시 자동으로 Token Refresh 시도
    if (error.response?.status === 401) {
      try {
        const refreshResponse = await refreshToken();
        if (refreshResponse.success) {
          // 새 Access Token 저장
          setAccessToken(refreshResponse.data.accessToken);
          // 원래 요청 재시도
          return axios.request(error.config);
        }
      } catch (refreshError) {
        // Refresh 실패 시 로그아웃 처리 및 로그인 페이지로 이동
        clearAuth();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
```

---

### 3단계: OAuth2 로그인 플로우 구현

#### 3-1. 로그인 페이지 구현
- "Google로 로그인" 버튼
- 클릭 시 `window.location.href = 'http://localhost:8080/oauth2/authorization/google'`

#### 3-2. Callback 페이지 구현 (`/auth/callback`)

**URL에서 `code` 파라미터 추출** (`useSearch` 또는 `URLSearchParams`)

**처리 로직**:
```typescript
const handleCallback = async (code: string) => {
  try {
    // 1. Authorization Code를 JWT 토큰으로 교환
    const response = await exchangeToken(code);

    // 2. BaseResponse에서 data 필드 추출
    if (response.success && response.data) {
      const { accessToken, tokenType, expiresIn } = response.data;

      // 3. Access Token을 Zustand 스토어에 저장
      setAccessToken(accessToken);

      // 4. 사용자 정보 조회 (BaseResponse 없이 직접 반환)
      const userInfo = await getCurrentUser();
      setUser(userInfo);

      // 5. 메인 페이지로 리다이렉트
      navigate('/dashboard');
    }
  } catch (error) {
    console.error('Login failed:', error);
    // 로그인 실패 메시지 표시
    navigate('/login?error=true');
  }
};

// error 파라미터가 있으면 로그인 페이지로 리다이렉트
if (errorParam) {
  navigate('/login?error=true');
}
```

#### 3-3. 세션 복원 구현 (`features/auth/hooks/useSessionRestore.ts`)

⚠️ **중요**: Access Token은 메모리에 저장되므로 페이지 새로고침 시 사라집니다. Refresh Token으로 세션을 복원해야 합니다.

**✅ React 18 + TanStack Query 모범 사례**: 데이터 페칭은 useEffect가 아닌 **TanStack Query**를 사용합니다.

**방법 1: TanStack Query 사용** ⭐ (권장)

```typescript
import { useQuery } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { refreshToken } from '@/api/auth';
import { getCurrentUser } from '@/api/user';

/**
 * 페이지 로드 시 Refresh Token으로 세션을 자동 복원
 * TanStack Query를 사용하여 로딩/에러 상태 자동 관리
 */
export function useSessionRestore() {
  const { setAccessToken, setUser } = useAuthStore();

  return useQuery({
    queryKey: ['session', 'restore'],
    queryFn: async () => {
      // 1. Refresh Token으로 새 Access Token 받기
      const response = await refreshToken();

      if (response.success && response.data) {
        // 2. Access Token 저장
        setAccessToken(response.data.accessToken);

        // 3. 사용자 정보 조회 (BaseResponse 없이 직접 반환)
        const userInfo = await getCurrentUser();
        setUser(userInfo);

        return userInfo;
      }

      throw new Error('No active session');
    },
    retry: false, // 세션 복원 실패 시 재시도 안 함
    staleTime: Infinity, // 캐시 무제한 유지
    refetchOnWindowFocus: false, // 포커스 시 재실행 방지
  });
}
```

**사용 방법**:

```typescript
// App.tsx
import { useSessionRestore } from '@/features/auth/hooks/useSessionRestore';

function App() {
  const { isLoading, isError } = useSessionRestore();

  // 로딩 중일 때 스피너 표시
  if (isLoading) {
    return <LoadingSpinner />;
  }

  return <RouterProvider router={router} />;
}
```

**장점**:
- ✅ 로딩/에러 상태 자동 관리 (`isLoading`, `isError`)
- ✅ useEffect 불필요
- ✅ 에러 처리 간결
- ✅ React 18 Concurrent Mode 호환
- ✅ 자동 캐싱 및 중복 요청 방지

---

**방법 2: useEffect 사용** (대안)

데이터 페칭이므로 TanStack Query 권장하지만, useEffect도 가능합니다:

```typescript
import { useEffect } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { refreshToken } from '@/api/auth';
import { getCurrentUser } from '@/api/user';

export function useSessionRestore() {
  const { setAccessToken, setUser } = useAuthStore();

  useEffect(() => {
    const restoreSession = async () => {
      try {
        const response = await refreshToken();
        if (response.success && response.data) {
          setAccessToken(response.data.accessToken);
          const userInfo = await getCurrentUser();
          setUser(userInfo);
        }
      } catch (error) {
        console.log('No active session');
      }
    };

    restoreSession();
  }, [setAccessToken, setUser]);
}
```

**단점**:
- ❌ 로딩 상태 수동 관리 필요
- ❌ 에러 처리 복잡
- ❌ useEffect 의존성 배열 관리

---

### 4단계: TanStack Query 통합

#### 4-1. Query 클라이언트 설정 (`lib/query-client.ts`)
- `QueryClient` 생성 및 기본 옵션 설정
- `staleTime`, `cacheTime`, `retry` 등 설정

#### 4-2. 인증 Mutation 작성

⚠️ **주의**: 인증 API는 `BaseResponse`로 감싸져 있으므로 `response.data` 필드를 추출해야 합니다.

```typescript
import { useMutation } from '@tanstack/react-query';
import { exchangeToken, refreshToken, logout } from '@/api/auth';

// Token 교환 Mutation
export function useExchangeToken() {
  return useMutation({
    mutationFn: (code: string) => exchangeToken(code),
    onSuccess: (response) => {
      if (response.success && response.data) {
        // BaseResponse.data에서 TokenResponse 추출
        const { accessToken } = response.data;
        setAccessToken(accessToken);
      }
    },
  });
}

// Token 갱신 Mutation
export function useRefreshToken() {
  return useMutation({
    mutationFn: () => refreshToken(),
    onSuccess: (response) => {
      if (response.success && response.data) {
        const { accessToken } = response.data;
        setAccessToken(accessToken);
      }
    },
  });
}

// 로그아웃 Mutation
export function useLogout() {
  return useMutation({
    mutationFn: () => logout(),
    onSuccess: (response) => {
      if (response.success) {
        clearAuth();
        navigate('/login');
      }
    },
  });
}
```

#### 4-3. 사용자 정보 Query 작성

⚠️ **주의**: `GET /api/users/me`는 `BaseResponse` 없이 `UserInfo`를 직접 반환합니다.

```typescript
import { useQuery } from '@tanstack/react-query';
import { getCurrentUser } from '@/api/user';

// 현재 사용자 정보 Query
export function useCurrentUser() {
  const { accessToken } = useAuthStore();

  return useQuery({
    queryKey: ['user', 'me'],
    queryFn: () => getCurrentUser(),
    enabled: !!accessToken, // Access Token이 있을 때만 실행
    staleTime: 5 * 60 * 1000, // 5분
  });
}
```

---

### 5단계: TanStack Router 통합

#### 5-1. 라우터 설정 (`lib/router.ts`)
- 라우트 정의 (로그인, Callback, 메인 페이지 등)
- 라우터 인스턴스 생성

#### 5-2. 보호된 라우트 구현
- 인증이 필요한 페이지에 Guard 추가
- `beforeLoad` 훅에서 인증 상태 확인
- 미인증 시 로그인 페이지로 리다이렉트

#### 5-3. 공개 라우트 구현
- 로그인 페이지, Callback 페이지는 인증 불필요
- 이미 로그인된 사용자가 접근 시 메인 페이지로 리다이렉트 (선택사항)

---

### 6단계: 자동 토큰 갱신 구현

#### 6-1. Token Refresh 전략
- Access Token 만료 5분 전에 자동 갱신 (권장)
- 또는 401 에러 시 갱신 (Response 인터셉터에서 처리)

#### 6-2. Refresh 로직 구현
```typescript
// Access Token 만료 시간 확인 (JWT 디코딩)
// 만료 5분 전이면 refreshToken() 호출
// 새 Access Token 저장
```

#### 6-3. 에러 처리
- Refresh Token 만료 시: 로그아웃 처리 및 로그인 페이지로 이동
- Refresh 실패 시: 에러 메시지 표시 및 재로그인 유도

---

### 7단계: 로그아웃 구현

#### 7-1. 로그아웃 함수 작성
1. `/api/auth/logout` API 호출
2. Zustand 스토어 초기화 (`clearAuth()`)
3. TanStack Query 캐시 초기화
4. 로그인 페이지로 리다이렉트

#### 7-2. UI 통합
- 헤더 또는 메뉴에 "로그아웃" 버튼 추가
- 클릭 시 로그아웃 함수 호출

---

## 보안 고려사항

### 1. Access Token 저장
- **권장**: Zustand 스토어 (메모리 기반)
- **비권장**: LocalStorage (XSS 공격에 취약)

### 2. Refresh Token
- HttpOnly 쿠키로 자동 관리됨 (JavaScript 접근 불가)
- CSRF 공격 방지를 위해 SameSite=Lax 설정됨

### 3. HTTPS 사용
- 프로덕션 환경에서는 반드시 HTTPS 사용
- 쿠키의 Secure 플래그 활성화

### 4. Authorization Code
- One-time use (1회용)
- 5분 후 자동 만료
- 사용 후 즉시 교환 필요

---

## 구현 체크리스트

### 필수 구현 항목

- [ ] BaseResponse 타입 정의 (`types/api.ts`)
- [ ] 인증 타입 정의 (`types/auth.ts`)
- [ ] API 클라이언트 설정 (`withCredentials: true`)
- [ ] 인증 Store (Zustand)
- [ ] 인증 API 함수 (`exchangeToken`, `refreshToken`, `logout`, `getCurrentUser`)
- [ ] Request 인터셉터 (Access Token 자동 추가)
- [ ] Response 인터셉터 (BaseResponse 처리 + 401 에러 시 Token Refresh)
- [ ] 로그인 페이지 (Google OAuth2 시작)
- [ ] Callback 페이지 (Authorization Code 처리)
- [ ] 세션 복원 커스텀 훅 (`useSessionRestore`)
- [ ] 보호된 라우트 Guard
- [ ] 로그아웃 기능

### 선택 구현 항목

- [ ] Access Token 자동 갱신 (만료 5분 전)
- [ ] 로딩 상태 UI
- [ ] 에러 토스트 또는 알림
- [ ] 리다이렉트 URL 저장 (로그인 후 원래 페이지로 복귀)

---

## 테스트 시나리오

### 정상 플로우
1. 로그인 버튼 클릭 → Google 로그인 → Callback 페이지 → 메인 페이지
2. API 호출 시 Access Token 자동 포함
3. Access Token 만료 시 자동 갱신
4. 로그아웃 → 토큰 삭제 → 로그인 페이지

### 에러 처리
1. Google 로그인 취소 → 에러 메시지 표시
2. 잘못된 Authorization Code → 에러 처리
3. Refresh Token 만료 → 재로그인 유도
4. 네트워크 에러 → 재시도 또는 에러 표시

---

## 참고: 주요 에러 코드

| 에러 코드 | 상황 | 처리 방법 |
|-----------|------|-----------|
| -10401 | 유효하지 않은 Access Token | Token Refresh 시도 |
| -10411 | Access Token 만료 | Token Refresh 시도 |
| -10415 | 유효하지 않은 Refresh Token | 재로그인 유도 |
| -10416 | Refresh Token 없음 | 재로그인 유도 |
| -10420 | Authorization Code 만료 | 재로그인 유도 |

---

## 환경 변수 설정

프론트엔드에서 필요한 환경 변수:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_OAUTH2_LOGIN_URL=http://localhost:8080/oauth2/authorization/google
```

---

## 디렉토리 구조 예시

```
src/
├── api/
│   ├── client.ts          # Axios/Fetch 클라이언트
│   ├── auth.ts            # 인증 API 함수
│   └── user.ts            # 사용자 API 함수
├── stores/
│   └── authStore.ts       # Zustand 인증 스토어
├── types/
│   ├── api.ts             # 공통 API 타입 (BaseResponse)
│   └── auth.ts            # 인증 관련 타입
├── lib/
│   ├── query-client.ts    # TanStack Query 클라이언트
│   └── router.ts          # TanStack Router 설정
├── features/
│   └── auth/
│       ├── hooks/
│       │   ├── useSessionRestore.ts # 세션 복원 커스텀 훅
│       │   └── useAuth.ts           # 기타 인증 관련 커스텀 훅
│       ├── components/
│       │   ├── LoginButton.tsx    # 로그인 버튼
│       │   └── LogoutButton.tsx   # 로그아웃 버튼
│       └── pages/
│           ├── LoginPage.tsx      # 로그인 페이지
│           └── CallbackPage.tsx   # OAuth2 Callback 페이지
└── routes/
    └── index.tsx          # 라우트 정의
```

---

## 추가 참고사항

1. **TanStack Query 장점**:
   - 자동 캐싱 및 백그라운드 갱신
   - 로딩/에러 상태 자동 관리
   - Mutation을 통한 낙관적 업데이트

2. **TanStack Router 장점**:
   - 타입 안전한 라우팅
   - 중첩 라우트 및 레이아웃 지원
   - 라우트 기반 코드 스플리팅

3. **Zustand 장점**:
   - 간단한 API 및 보일러플레이트 최소화
   - TypeScript 친화적
   - DevTools 지원

4. **CORS 설정**:
   - 현재 백엔드는 개발 환경에서 모든 Origin 허용
   - `withCredentials: true` 설정 필수 (쿠키 전송)
