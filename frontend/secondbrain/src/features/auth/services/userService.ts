import apiClient from '@/api/client';
import type { UserInfo } from '@/features/auth/types/auth';

/**
 * GET /api/users/me
 * 현재 로그인된 사용자 정보 조회
 * ⚠️ 주의: 이 API는 BaseResponse 없이 UserInfo를 직접 반환
 * @returns UserInfo
 */
export async function getCurrentUser(): Promise<UserInfo> {
  const response = await apiClient.get<UserInfo>('/api/users/me');
  return response.data;
}
