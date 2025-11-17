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

  /** 배치 ID (같은 Save 클릭으로 저장된 항목들) */
  batchId: string;

  /** 배치 타임스탬프 */
  batchTimestamp: number;
}

/**
 * 저장 시작 브로드캐스트 메시지
 */
export interface SaveStatusStartedMessage {
  type: 'SAVE_STATUS_STARTED';
  urls: string[];
  batchId: string;
  batchTimestamp: number;
}

/**
 * 저장 완료 브로드캐스트 메시지
 */
export interface SaveStatusCompletedMessage {
  type: 'SAVE_STATUS_COMPLETED';
  urls: string[];
  batchId: string;
  success: boolean;
  error?: string;
}
