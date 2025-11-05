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
content: String (필수, 최대 2048자)
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
content: String (필수, 최대 2048자)
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
