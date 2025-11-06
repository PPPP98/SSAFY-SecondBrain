import { apiClient } from '@/api/client';
import type { BaseResponse } from '@/shared/types/api';

/**
 * POST /api/users/reminder
 * 사용자 리마인더 알림 설정 토글
 * 현재 알림 상태를 반대로 변경 (On ↔ Off)
 * @returns Promise<void>
 */
export async function toggleReminder(): Promise<void> {
  await apiClient.post<BaseResponse<void>>('/api/users/reminder');
}
