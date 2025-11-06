# Note API 문서

## 목차

1. [노트 기본 CRUD API](#노트-기본-crud-api)
2. [노트 리마인더 API](#노트-리마인더-api)
3. [노트 검색 API (Elasticsearch)](#노트-검색-api-elasticsearch)

---

## 노트 기본 CRUD API

### 1. 노트 생성

**API명**: `POST /api/notes`

**Content-Type**: `multipart/form-data`

**Request Body** (Form Data):

```
title: String (필수, 최대 64자)
content: String (필수, TEXT 타입 무제한)
images: MultipartFile[] (선택)
```

**Request 예시**:

```
title: "멀티 테스트"
content: "멀티 테스트 내용"
images: [file1.jpg, file2.png]
```

**Response**:

```json
{
  "success": true,
  "code": 201,
  "message": "Created",
  "data": null
}
```

**HTTP Status**: `201 Created`

**설명**:

- JWT 토큰으로 인증된 사용자의 노트를 생성합니다.
- 이미지는 선택사항이며, 여러 개의 이미지를 첨부할 수 있습니다.

---

### 2. 노트 조회

**API명**: `GET /api/notes/{noteId}`

**Path Variable**:

- `noteId`: Long (조회할 노트 ID)

**Request Body**: 없음

**Response**:

```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": {
    "noteId": 1,
    "title": "노트 제목",
    "content": "노트 내용",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T11:00:00",
    "remindAt": "2025-01-16T10:30:00",
    "remindCount": 0
  }
}
```

**HTTP Status**: `200 OK`

**설명**:

- 본인의 노트만 조회 가능합니다.
- 다른 사용자의 노트는 접근이 거부됩니다.

---

### 3. 노트 수정

**API명**: `PUT /api/notes/{noteId}`

**Content-Type**: `multipart/form-data`

**Path Variable**:

- `noteId`: Long (수정할 노트 ID)

**Request Body** (Form Data):

```
title: String (필수, 최대 64자)
content: String (필수, TEXT 타입 무제한)
images: MultipartFile[] (선택)
```

**Request 예시**:

```
title: "수정된 제목"
content: "수정된 내용"
images: [new_file.jpg]
```

**Response**:

```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": {
    "noteId": 1,
    "title": "수정된 제목",
    "content": "수정된 내용",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T12:00:00",
    "remindAt": null,
    "remindCount": 0
  }
}
```

**HTTP Status**: `200 OK`

**설명**:

- 본인의 노트만 수정 가능합니다.
- 다른 사용자의 노트는 접근이 거부됩니다.

---

### 4. 노트 삭제 (단일 및 다중 삭제)

**API명**: `DELETE /api/notes`

**Request Body**:

```json
{
  "noteIds": [1, 2, 3]
}
```

**Request 필드 설명**:

- `noteIds`: Long[] (필수, 최소 1개 이상)

**Response**:

```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": null
}
```

**HTTP Status**: `200 OK`

**설명**:

- 단일 또는 여러 개의 노트를 한 번에 삭제할 수 있습니다.
- 본인의 노트만 삭제 가능합니다.

---

### 5. 최근 노트 목록 조회

**API명**: `GET /api/notes/recent`

**Request Body**: 없음

**Query Parameters**: 없음

**Response**:

```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": [
    {
      "noteId": 5,
      "title": "최근 노트 5"
    },
    {
      "noteId": 4,
      "title": "최근 노트 4"
    },
    {
      "noteId": 3,
      "title": "최근 노트 3"
    }
  ]
}
```

**HTTP Status**: `200 OK`

**노트가 없는 경우 Response**:

```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": null
}
```

**설명**:

- 최근 수정된 노트 상위 10개를 조회합니다.
- `updatedAt` 기준 내림차순, 동일 시 `noteId` 기준 내림차순으로 정렬됩니다.
- **노트가 없을 경우 `data`는 `null`입니다** (빈 배열이 아님).

---

## 노트 리마인더 API

### 6. 개별 노트 리마인더 활성화

**API명**: `POST /api/notes/{noteId}/reminders`

**Path Variable**:

- `noteId`: Long (리마인더를 활성화할 노트 ID)

**Request Body**: 없음

**Response**:

```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": null
}
```

**HTTP Status**: `200 OK`

**설명**:

- 망각 곡선 기반으로 3회 리마인더를 발송합니다.
- 본인의 노트에만 리마인더를 설정할 수 있습니다.

---

### 7. 개별 노트 리마인더 비활성화

**API명**: `DELETE /api/notes/{noteId}/reminders`

**Path Variable**:

- `noteId`: Long (리마인더를 비활성화할 노트 ID)

**Request Body**: 없음

**Response**:

```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": null
}
```

**HTTP Status**: `200 OK`

**설명**:

- 해당 노트의 리마인더를 중단하고 리마인드 횟수를 0으로 초기화합니다.
- 본인의 노트에만 적용할 수 있습니다.

---

### 8. 리마인더 활성화 노트 목록 조회

**API명**: `GET /api/notes/reminders`

**Query Parameters**:

- `page`: int (페이지 번호, 0부터 시작, 기본값: 0)
- `size`: int (페이지당 노트 개수, 기본값: 10)

**Request 예시**:

```
GET /api/notes/reminders?page=0&size=10
```

**Response**:

```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": {
    "results": [
      {
        "noteId": 10,
        "title": "리마인더 노트 1"
      },
      {
        "noteId": 9,
        "title": "리마인더 노트 2"
      }
    ],
    "totalCount": 25,
    "currentPage": 0,
    "totalPages": 3,
    "pageSize": 10
  }
}
```

**Response 필드 설명**:

- `results`: 리마인더 노트 목록 (noteId, title)
- `totalCount`: 전체 리마인더 노트 개수
- `currentPage`: 현재 페이지 번호
- `totalPages`: 전체 페이지 수
- `pageSize`: 페이지당 노트 개수

**HTTP Status**: `200 OK`

**설명**:

- 무한스크롤을 위한 페이지네이션을 지원합니다.
- `updatedAt` 기준 내림차순, 동일 시 `noteId` 기준 내림차순으로 정렬됩니다.

---

## 노트 검색 API (Elasticsearch)

### 9. 노트 검색

**API명**: `GET /api/notes/search`

**Query Parameters**:

- `keyword`: String (필수, 검색 키워드)
- `page`: int (페이지 번호, 0부터 시작, 기본값: 0)
- `size`: int (페이지당 노트 개수, 기본값: 10)

**Request 예시**:

```
GET /api/notes/search?keyword=테스트&page=0&size=10
```

**Response**:

```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": {
    "results": [
      {
        "id": 15,
        "title": "테스트 노트",
        "content": "테스트 내용입니다.",
        "userId": 1,
        "createdAt": "2025-01-15T10:30:00",
        "updatedAt": "2025-01-15T11:00:00",
        "remindCount": 0
      },
      {
        "id": 12,
        "title": "유사 테스트",
        "content": "비슷한 내용",
        "userId": 1,
        "createdAt": "2025-01-14T09:20:00",
        "updatedAt": "2025-01-14T09:45:00",
        "remindCount": 1
      }
    ],
    "totalCount": 18,
    "currentPage": 0,
    "totalPages": 2,
    "pageSize": 10
  }
}
```

**Response 필드 설명**:

- `results`: 검색 결과 노트 목록
  - `id`: 노트 ID
  - `title`: 노트 제목
  - `content`: 노트 내용
  - `userId`: 사용자 ID
  - `createdAt`: 생성 시간
  - `updatedAt`: 수정 시간
  - `remindCount`: 리마인더 횟수
- `totalCount`: 전체 검색 결과 개수
- `currentPage`: 현재 페이지 번호
- `totalPages`: 전체 페이지 수
- `pageSize`: 페이지당 노트 개수

**HTTP Status**: `200 OK`

**설명**:

- Elasticsearch를 사용하여 제목과 내용을 기반으로 검색합니다.
- 키워드와 유사한 노트도 함께 검색됩니다.
- 본인의 노트만 검색됩니다.

---

### 10. 유사 노트 검색

**API명**: `GET /api/notes/{noteId}/similar`

**Path Variable**:

- `noteId`: Long (기준 노트 ID)

**Query Parameters**:

- `limit`: int (조회할 유사 노트 개수, 기본값: 5)

**Request 예시**:

```
GET /api/notes/15/similar?limit=5
```

**Response**:

```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": [
    {
      "id": 18,
      "title": "유사한 노트 1",
      "content": "비슷한 내용...",
      "userId": 1,
      "createdAt": "2025-01-14T10:00:00",
      "updatedAt": "2025-01-14T10:30:00",
      "remindCount": 0
    },
    {
      "id": 16,
      "title": "유사한 노트 2",
      "content": "관련된 내용...",
      "userId": 1,
      "createdAt": "2025-01-13T15:20:00",
      "updatedAt": "2025-01-13T15:45:00",
      "remindCount": 2
    }
  ]
}
```

**Response 필드 설명**:

- `data`: 유사 노트 목록 (배열)
  - `id`: 노트 ID
  - `title`: 노트 제목
  - `content`: 노트 내용
  - `userId`: 사용자 ID
  - `createdAt`: 생성 시간
  - `updatedAt`: 수정 시간
  - `remindCount`: 리마인더 횟수

**HTTP Status**: `200 OK`

**설명**:

- 지정한 노트와 유사한 노트를 Elasticsearch를 통해 검색합니다.
- 기본적으로 5개의 유사 노트를 반환하며, `limit` 파라미터로 조정 가능합니다.
- 본인의 노트 중에서만 유사 노트를 찾습니다.

---

## 공통 응답 형식

모든 API는 다음과 같은 공통 응답 형식을 사용합니다:

```json
{
  "success": boolean,
  "code": int,
  "message": String,
  "data": T | null
}
```

**필드 설명**:

- `success`: 요청 성공 여부 (true/false)
- `code`: HTTP 상태 코드
- `message`: 응답 메시지
- `data`: 응답 데이터 (없을 경우 null)

**인증**:

- 모든 API는 JWT 인증이 필요합니다.
- Authorization 헤더에 Bearer 토큰을 포함해야 합니다.
- 예: `Authorization: Bearer <access_token>`

---

## 에러 응답

인증 실패나 권한 부족 등의 에러 발생 시 다음과 같은 형식으로 응답합니다:

```json
{
  "success": false,
  "code": 401,
  "message": "Unauthorized",
  "data": null
}
```

**주요 에러 코드**:

- `400`: Bad Request (잘못된 요청)
- `401`: Unauthorized (인증 실패)
- `403`: Forbidden (권한 없음)
- `404`: Not Found (리소스 없음)
- `500`: Internal Server Error (서버 오류)

---

## 노트 Draft API (자동 저장)

### 11. Draft 저장 (Auto-save)

**API명**: `POST /api/drafts`

**Request Body**:

```json
{
  "noteId": "123e4567-e89b-12d3-a456-426614174000",
  "title": "제목",
  "content": "내용",
  "version": 1
}
```

**Request 필드 설명**:

- `noteId`: String (선택, UUID 형식. null이면 서버에서 생성)
- `title`: String (선택, 타이핑 중 빈 값 허용)
- `content`: String (선택, 타이핑 중 빈 값 허용)
- `version`: Long (필수, 충돌 감지용)
  - 새 Draft: version = 1
  - 기존 Draft 수정: 현재 version 전송 필수
  - 서버에서 버전 불일치 시 DRAFT_VERSION_CONFLICT 에러 반환
  - Optimistic Locking을 통한 동시 편집 보호

**검증**: title 또는 content 중 **하나라도 있어야 함**

**Response**:

```json
{
  "isSuccess": true,
  "code": 201,
  "message": "요청에 성공하였습니다.",
  "data": "123e4567-e89b-12d3-a456-426614174000"
}
```

**HTTP Status**: `201 Created`

**설명**:

- 프론트엔드 Debouncing (500ms) 후 호출됩니다.
- Redis에 24시간 TTL로 임시 저장됩니다.
- title과 content가 둘 다 비어있으면 저장되지 않습니다.

---

### 12. Draft 목록 조회

**API명**: `GET /api/drafts`

**Request Body**: 없음

**Response**:

```json
{
  "isSuccess": true,
  "code": 1000,
  "message": "요청에 성공하였습니다.",
  "data": {
    "drafts": [
      {
        "noteId": "123e4567-e89b-12d3-a456-426614174000",
        "title": "첫 번째 노트",
        "content": "내용",
        "version": 3,
        "lastModified": "2025-01-06T10:30:00"
      },
      {
        "noteId": "223e4567-e89b-12d3-a456-426614174001",
        "title": "두 번째 노트",
        "content": "내용",
        "version": 1,
        "lastModified": "2025-01-06T10:25:00"
      }
    ],
    "totalCount": 2
  }
}
```

**HTTP Status**: `200 OK`

**설명**:

- 사용자의 모든 Draft를 조회합니다.
- 브라우저 재시작 후 미저장 Draft 복구에 사용됩니다.
- lastModified 기준 내림차순으로 정렬됩니다.

---

### 13. Draft 조회

**API명**: `GET /api/drafts/{noteId}`

**Path Variable**:

- `noteId`: String (UUID 형식)

**Response**:

```json
{
  "isSuccess": true,
  "code": 1000,
  "message": "요청에 성공하였습니다.",
  "data": {
    "noteId": "123e4567-e89b-12d3-a456-426614174000",
    "title": "제목",
    "content": "내용",
    "version": 3,
    "lastModified": "2025-01-06T10:30:00"
  }
}
```

**HTTP Status**: `200 OK`

**설명**:

- Side Peek 열릴 때 또는 페이지 로드 시 호출됩니다.
- 본인의 Draft만 조회 가능합니다.

---

### 14. Draft 삭제

**API명**: `DELETE /api/drafts/{noteId}`

**Path Variable**:

- `noteId`: String (UUID 형식)

**Response**:

```json
{
  "isSuccess": true,
  "code": 1000,
  "message": "요청에 성공하였습니다.",
  "data": null
}
```

**HTTP Status**: `200 OK`

**설명**:

- DB 저장 후 또는 취소 시 호출됩니다.
- 본인의 Draft만 삭제 가능합니다.

---

### 15. Draft → DB 자동 저장

**API명**: `POST /api/notes/from-draft/{noteId}`

**Path Variable**:

- `noteId`: String (UUID 형식)

**Request Body**: 없음

**Response**:

```json
{
  "isSuccess": true,
  "code": 201,
  "message": "요청에 성공하였습니다.",
  "data": {
    "id": 42,
    "title": "제목",
    "content": "내용",
    "createdAt": "2025-01-06T10:30:00",
    "updatedAt": "2025-01-06T10:30:00"
  }
}
```

**HTTP Status**: `201 Created`

**자동 저장 트리거** (명시적 저장 버튼 없음):

1. 프론트엔드 Batching (50회 변경 또는 5분 경과)
2. 페이지 이탈 (beforeunload)
3. Side Peek 닫기
4. 백엔드 스케줄러 (5분마다)

**설명**:

- Draft를 DB에 영구 저장합니다.
- title과 content가 **모두** 필수입니다 (빈 값 불가).
- 저장 성공 시 Redis에서 Draft가 자동 삭제됩니다.

---

## Draft API 에러 코드

| 코드       | HTTP Status               | 메시지                                 |
| ---------- | ------------------------- | -------------------------------------- |
| **-10800** | 404 NOT_FOUND             | Draft를 찾을 수 없습니다.              |
| **-10801** | 403 FORBIDDEN             | Draft 접근 권한이 없습니다.            |
| **-10802** | 409 CONFLICT              | Draft 버전 충돌 (다른 기기에서 수정됨) |
| **-10803** | 400 BAD_REQUEST           | 제목과 내용 중 하나는 필수입니다.      |
| **-10804** | 500 INTERNAL_SERVER_ERROR | Redis 저장소 오류                      |
| **-10805** | 400 BAD_REQUEST           | 버전 정보는 필수입니다.                |
| **-10806** | 400 BAD_REQUEST           | 잘못된 버전 정보입니다.                |

---

## 실시간 노트 자동 저장 시스템 아키텍처

### 백엔드 구현 방식

**Write-Behind 패턴 + Snapshot 방식**:

```
사용자 타이핑
    ↓ (onChange event)
프론트엔드 Debouncing (500ms)
    ↓
검증: title OR content 중 하나라도 있음?
    ↓ ✅ Yes
POST /api/drafts (Redis 저장, TTL 24h)
    ↓
자동 저장 트리거 (4가지 중 하나):
    1. 50회 변경
    2. 5분 경과
    3. 페이지 이탈 (beforeunload)
    4. Side Peek 닫기
    ↓
POST /api/notes/from-draft/{noteId}
    ↓
검증: title AND content 모두 필수
    ↓ ✅ Pass
PostgreSQL 저장 (영구)
    ↓
    ├─ Elasticsearch 인덱싱 (비동기)
    └─ Draft 삭제 (Redis)
```

**백엔드 스케줄러** (이중 안전장치):

```
5분마다 실행
    ↓
getStaleDrafts(5) // 5분 이상 지난 Draft 조회
    ↓
각 Draft → DB 저장 시도
    ↓ (검증 실패 시 건너뜀)
성공 시 Draft 삭제
```

**핵심 특징**:

- **Snapshot 방식**: 전체 title + content 저장 (Delta 아님)
- **2단계 검증**: Redis (유연) → DB (엄격)
- **Optimistic Locking**: Version 기반 충돌 감지
- **Best-Effort 삭제**: Redis 삭제 실패해도 TTL로 자동 처리

---

### 프론트엔드 구현 플로우

**1단계: 초기화 및 Draft 로드**

```
Side Peek 열기
    ↓
새 노트인가?
    ├─ Yes → UUID 생성, 빈 상태로 시작
    └─ No  → GET /api/drafts/{noteId} 호출
        ↓
        Draft 존재?
        ├─ Yes → Draft 데이터로 에디터 초기화
        └─ No  → 빈 상태로 시작
```

**2단계: 실시간 자동 저장 (Redis)**

```
onChange 이벤트 (title 또는 content 변경)
    ↓
Refs 업데이트 (titleRef, contentRef)
    ↓
Debouncing 함수 호출 (500ms 대기)
    ↓
타이핑 멈춤 후 500ms 경과
    ↓
검증: title.trim() || content.trim() 있음?
    ├─ No  → 저장 안 함 (메모리에만 유지)
    └─ Yes → POST /api/drafts 호출
        ↓
        성공 시:
        ├─ version 증가
        ├─ changeCount 증가
        └─ DB 저장 조건 체크

        실패 시:
        └─ LocalStorage Fallback (선택사항)
```

**3단계: Batching (자동 DB 저장)**

```
매 Redis 저장 후 체크:
    ↓
조건 확인:
    - changeCount >= 50 OR
    - timeSinceLastSave >= 5분
    ↓ ✅ 조건 충족
검증: title.trim() && content.trim() 둘 다 있음?
    ├─ No  → DB 저장 안 함 (빈 노트)
    └─ Yes → POST /api/notes/from-draft/{noteId}
        ↓
        성공 시:
        ├─ changeCount 초기화
        ├─ lastSaveTime 업데이트
        └─ Draft 자동 삭제됨
```

**4단계: 페이지 이탈 처리**

```
beforeunload 이벤트
    ↓
title.trim() && content.trim() 둘 다 있음?
    ├─ No  → 아무것도 안 함 (메모리 소멸)
    └─ Yes → navigator.sendBeacon() 사용
        └─ POST /api/notes/from-draft/{noteId}
            (비동기, 페이지 종료 전 보장)
```

**5단계: Side Peek 닫기**

```
onClose 이벤트
    ↓
내용 확인:
    ├─ title.trim() && content.trim() 둘 다 있음
    │   └─ POST /api/notes/from-draft/{noteId}
    │       (DB 저장 후 자동으로 Draft 삭제됨)
    │
    └─ 빈 노트 (둘 다 없거나 하나만 있음)
        └─ version > 1 (Redis에 저장된 적 있음)?
            ├─ Yes → DELETE /api/drafts/{noteId}
            │        (입력했다가 지운 케이스)
            └─ No  → 아무것도 안 함
                     (완전히 빈 노트, 메모리에서 자동 소멸)
```

**상태 관리**:

- `titleRef`: 현재 제목 (메모리)
- `contentRef`: 현재 내용 (메모리)
- `versionRef`: Draft 버전 (충돌 감지)
- `changeCountRef`: 변경 횟수 (Batching)
- `lastDbSaveTimeRef`: 마지막 DB 저장 시간 (Batching)

**핵심 원칙**:

1. **완전히 빈 노트**: Redis 저장 안 됨 (메모리에만 존재)
2. **부분적으로 채워진 노트**: Redis 저장 (title 또는 content 중 하나)
3. **완성된 노트**: DB 저장 (title과 content 모두 필수)
4. **자동 저장**: 명시적 "저장" 버튼 없음 (Notion 방식)

**성능 최적화**:

- Debouncing: 타이핑 중 API 호출 99% 감소
- Batching: DB Write 99.9% 감소
- Snapshot 방식: 구현 간단, 버그 적음
- Redis 효율성: 빈 노트 저장 안 함 (70% 절감)

---
