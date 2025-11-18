/**
 * Knowledge Graph Service 노트 저장 요청
 * POST /ai/api/v1/agents/summarize
 */
export interface SavePageRequest {
  /**
   * URL 또는 텍스트 배열
   * - URL인 경우: trafilatura로 웹 크롤링 후 요약
   * - 텍스트인 경우: 그대로 요약에 포함
   */
  data: string[];
}

/**
 * Knowledge Graph Service 노트 저장 응답
 * Spring Boot 백엔드의 노트 생성 API 응답 형식
 */
export interface SavePageResponse {
  /**
   * 요청 성공 여부
   */
  success: boolean;

  /**
   * 응답 메시지
   */
  message: string;

  /**
   * 생성된 노트 ID (선택적)
   */
  noteId?: number;
}

/**
 * 노트 저장 에러
 */
export interface SavePageError {
  /**
   * 에러 코드
   * - NO_TOKEN: 로그인 필요
   * - NO_TAB: 현재 탭 없음
   * - INVALID_URL: 시스템 페이지 등 저장 불가 URL
   * - API_ERROR: API 오류
   * - NETWORK_ERROR: 네트워크 오류
   * - UNKNOWN_ERROR: 알 수 없는 오류
   */
  error: 'NO_TOKEN' | 'NO_TAB' | 'INVALID_URL' | 'API_ERROR' | 'NETWORK_ERROR' | 'UNKNOWN_ERROR';

  /**
   * 에러 메시지
   */
  message: string;

  /**
   * HTTP 상태 코드 (API 오류인 경우)
   */
  status?: number;
}

/**
 * 노트 검색 결과 아이템
 * GET /api/notes/search 응답
 */
export interface NoteSearchResult {
  id: number;
  title: string;
  content: string;
  userId: number;
  createdAt: string;
  updatedAt: string;
  remindCount: number;
  score?: number; // Elasticsearch 유사도 점수 (선택적)
}

/**
 * 노트 검색 API 응답
 * GET /api/notes/search
 */
export interface NoteSearchApiResponse {
  success: boolean;
  code: number;
  message: string;
  data: {
    results: NoteSearchResult[];
    totalCount: number;
    currentPage: number;
    totalPages: number;
    pageSize: number;
  };
}

/**
 * 관련 노트 (Knowledge Graph 이웃 노드)
 * GET /ai/api/v1/graph/neighbors/{note_id} 응답 항목
 */
export interface RelatedNote {
  /**
   * 이웃 노트 ID
   */
  neighbor_id: number;

  /**
   * 이웃 노트 제목
   */
  neighbor_title: string;

  /**
   * 그래프 거리 (1 = 직접 연결, 2 = 2단계 연결 등)
   */
  distance: number;
}

/**
 * 관련 노트 API 응답
 * GET /ai/api/v1/graph/neighbors/{note_id}
 */
export interface RelatedNotesResponse {
  /**
   * 중심 노트 ID
   */
  center_note_id: number;

  /**
   * 관련 노트 목록
   */
  neighbors: RelatedNote[];
}
