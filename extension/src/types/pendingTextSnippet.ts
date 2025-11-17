/**
 * 임시 저장된 텍스트 조각
 * 드래그하여 추가한 텍스트들을 누적 저장
 */
export interface PendingTextSnippet {
  /**
   * 고유 식별자 (UUID)
   */
  id: string;

  /**
   * 드래그하여 선택된 텍스트
   */
  text: string;

  /**
   * 출처 URL
   */
  sourceUrl: string;

  /**
   * 페이지 제목
   */
  pageTitle: string;

  /**
   * 추가된 시간 (timestamp)
   */
  timestamp: number;
}

/**
 * 임시 텍스트 조각 추가 메시지
 */
export interface AddTextSnippetMessage {
  type: 'ADD_TEXT_SNIPPET';
  snippet: PendingTextSnippet;
}

/**
 * 임시 텍스트 조각 추가 응답
 */
export interface AddTextSnippetResponse {
  success: boolean;
  duplicate?: boolean;
  error?: string;
  count?: number; // 현재 누적된 총 개수
}
