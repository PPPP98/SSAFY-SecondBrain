# API 명세서

## Base URL
```
http://localhost:8080
```

## 공통 응답 형식

대부분의 API는 다음과 같은 공통 응답 구조(`BaseResponse`)를 사용합니다:

```json
{
  "success": true,
  "code": 200,
  "message": "요청에 성공했습니다.",
  "data": { /* 응답 데이터 */ }
}
```

**예외:** `GET /api/users/me`는 `BaseResponse`로 감싸지 않고 `UserResponse`를 직접 반환합니다.

### 에러 응답 형식

모든 API의 에러 응답은 `BaseResponse<Void>` 형식으로 통일됩니다:

```json
{
  "success": false,
  "code": -10401,
  "message": "유효하지 않은 액세스 토큰입니다.",
  "data": null
}
```

## 인증 API

### 1. 토큰 발급 (Authorization Code 교환)

Google OAuth2 로그인 후 받은 Authorization Code를 JWT 토큰으로 교환합니다.

**Endpoint:** `POST /api/auth/token`

**Request:**
```http
POST /api/auth/token?code={authorizationCode}
Content-Type: application/json
```

**Request Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| code | string | O | OAuth2 인증 후 받은 Authorization Code |

**Response:**
```json
{
  "success": true,
  "code": 200,
  "message": "요청에 성공했습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

**Response Headers:**
```http
Set-Cookie: refreshToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=604800
```

**Response 필드:**
| 필드 | 타입 | 설명 |
|------|------|------|
| accessToken | string | 액세스 토큰 (1시간 유효) |
| tokenType | string | 토큰 타입 (항상 "Bearer") |
| expiresIn | number | 만료 시간 (초 단위, 3600 = 1시간) |

**에러 응답:**
| 에러 코드 | HTTP 상태 | 메시지 |
|-----------|-----------|--------|
| -10421 | 400 | 인증 코드가 제공되지 않았습니다. |
| -10420 | 401 | 유효하지 않거나 만료된 인증 코드입니다. |
| -10402 | 404 | 존재하지 않는 사용자입니다. |
| -10500 | 500 | 요청 처리 중 서버 오류가 발생했습니다. |

---

### 2. 토큰 갱신 (Refresh)

만료된 Access Token을 새로운 토큰으로 갱신합니다.

**Endpoint:** `POST /api/auth/refresh`

**Request:**
```http
POST /api/auth/refresh
Cookie: refreshToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "success": true,
  "code": 200,
  "message": "요청에 성공했습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

**Response Headers:**
```http
Set-Cookie: refreshToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=604800
```

**에러 응답:**
| 에러 코드 | HTTP 상태 | 메시지 |
|-----------|-----------|--------|
| -10416 | 401 | 리프레시 토큰을 찾을 수 없습니다. |
| -10415 | 401 | 유효하지 않은 리프레시 토큰입니다. |
| -10402 | 404 | 존재하지 않는 사용자입니다. |

---

### 3. 로그아웃

현재 사용자의 Refresh Token을 무효화하고 쿠키를 삭제합니다.

**Endpoint:** `POST /api/auth/logout`

**Request:**
```http
POST /api/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Cookie: refreshToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "success": true,
  "code": 200,
  "message": "요청에 성공했습니다.",
  "data": null
}
```

**Response Headers:**
```http
Set-Cookie: refreshToken=; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=0
```

---

## 사용자 API

### 1. 현재 사용자 정보 조회

로그인한 사용자의 정보를 조회합니다.

**Endpoint:** `GET /api/users/me`

**Request:**
```http
GET /api/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**

⚠️ **주의:** 이 API는 `BaseResponse`로 감싸지 않고 `UserResponse`를 직접 반환합니다.

```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "picture": "https://lh3.googleusercontent.com/a/...",
  "setAlarm": true
}
```

**Response 필드:**
| 필드 | 타입 | 설명 |
|------|------|------|
| id | number | 사용자 ID |
| email | string | 이메일 주소 |
| name | string | 사용자 이름 |
| picture | string | 프로필 이미지 URL |
| setAlarm | boolean | 알림 설정 여부 |

**에러 응답:**

에러 발생 시에는 `BaseResponse<Void>` 형식으로 반환됩니다:

```json
{
  "success": false,
  "code": -10401,
  "message": "유효하지 않은 액세스 토큰입니다.",
  "data": null
}
```

| 에러 코드 | HTTP 상태 | 메시지 |
|-----------|-----------|--------|
| -10401 | 401 | 유효하지 않은 액세스 토큰입니다. |
| -10411 | 401 | 토큰이 만료되었습니다. 다시 로그인해주세요. |

---

## OAuth2 로그인

### Google OAuth2 로그인 시작

**Endpoint:** `GET /oauth2/authorization/google`

**설명:**
- 사용자를 Google 로그인 페이지로 리다이렉트합니다.
- 로그인 성공 시 프론트엔드 Callback URL로 리다이렉트됩니다.

**Redirect URL (성공):**
```
{OAUTH2_REDIRECT_URL}/auth/callback?code={authorizationCode}
```

**Redirect URL (실패):**
```
{OAUTH2_FAILURE_REDIRECT_URL}?error=true
```

---

## 에러 코드 전체 목록

### 인증/인가 에러
| 코드 | HTTP 상태 | 메시지 |
|------|-----------|--------|
| -10401 | 401 | 유효하지 않은 액세스 토큰입니다. |
| -10402 | 404 | 존재하지 않는 사용자입니다. |
| -10403 | 403 | 로그인이 필요한 서비스입니다. |
| -10405 | 401 | 로그인이 취소되었습니다. 다시 시도해주세요. |
| -10406 | 503 | 인증 서버 연결에 실패했습니다. 잠시 후 다시 시도해주세요. |
| -10407 | 500 | 로그인 중 오류가 발생했습니다. 다시 시도해주세요. |

### JWT 토큰 에러
| 코드 | HTTP 상태 | 메시지 |
|------|-----------|--------|
| -10411 | 401 | 토큰이 만료되었습니다. 다시 로그인해주세요. |
| -10412 | 401 | 유효하지 않은 토큰입니다. |
| -10413 | 401 | 토큰 서명이 유효하지 않습니다. |
| -10414 | 500 | 인증 처리 중 오류가 발생했습니다. |

### Refresh Token 및 인증 코드 에러
| 코드 | HTTP 상태 | 메시지 |
|------|-----------|--------|
| -10415 | 401 | 유효하지 않은 리프레시 토큰입니다. |
| -10416 | 401 | 리프레시 토큰을 찾을 수 없습니다. |
| -10420 | 401 | 유효하지 않거나 만료된 인증 코드입니다. |
| -10421 | 400 | 인증 코드가 제공되지 않았습니다. |

### 일반 에러
| 코드 | HTTP 상태 | 메시지 |
|------|-----------|--------|
| -10400 | 400 | 잘못된 요청입니다. |
| -10500 | 500 | 요청 처리 중 서버 오류가 발생했습니다. |
| -10503 | 500 | 예기치 못한 이유로 오류가 발생했습니다. |

---

## CORS 설정

현재 개발 환경에서는 모든 Origin을 허용합니다.

**허용된 HTTP 메소드:**
- GET
- POST
- PUT
- DELETE
- PATCH
- OPTIONS

**허용된 헤더:**
- 모든 헤더 허용

**자격 증명(Credentials):**
- 쿠키 및 Authorization 헤더 허용

---

## 보안 참고사항

1. **Access Token**:
   - JSON Response Body로 전달
   - 로컬 스토리지 또는 메모리에 저장
   - 유효 기간: 1시간
   - API 요청 시 `Authorization: Bearer {token}` 헤더로 전송

2. **Refresh Token**:
   - HttpOnly Cookie로 전달 (JavaScript 접근 불가)
   - 자동으로 쿠키에 저장됨
   - 유효 기간: 7일
   - Refresh 요청 시 자동으로 전송됨

3. **Authorization Code**:
   - One-time use (1회용)
   - 유효 기간: 5분
   - 사용 후 즉시 삭제됨
