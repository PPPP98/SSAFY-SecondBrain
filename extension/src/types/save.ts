/**
 * 저장 요청 상태
 */
export type SaveRequestStatus = 'pending' | 'saving' | 'success' | 'error';

/**
 * 개별 저장 요청 정보
 */
export interface SaveRequest {
  /** 고유 식별자 */
  id: string;

  /** 저장할 URL */
  url: string;

  /** 현재 상태 */
  status: SaveRequestStatus;

  /** 요청 시작 시간 */
  startTime: number;

  /** 완료 시간 (선택적) */
  completedTime?: number;

  /** 에러 메시지 (선택적) */
  error?: string;
}
