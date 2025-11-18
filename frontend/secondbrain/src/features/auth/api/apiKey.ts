import { apiClient } from '@/api/client';
import type { BaseResponse } from '@/shared/types/api';
import type { ApiKeyResponse } from '@/features/auth/types/apiKey';

/**
 * API Key 발급 (또는 재발급)
 * POST /api/apikey
 *
 * @returns {Promise<string>} 발급된 API Key (UUID 형식)
 * @throws {Error} API Key 발급 실패 시
 */
export async function generateApiKey(): Promise<string> {
  const response = await apiClient.post<BaseResponse<ApiKeyResponse>>('/api/apikey');

  if (!response.data.success || !response.data.data) {
    throw new Error('API Key 발급에 실패했습니다.');
  }

  return response.data.data.apiKey;
}

/**
 * API Key 삭제
 * DELETE /api/apikey
 *
 * @returns {Promise<void>}
 * @throws {Error} API Key 삭제 실패 시
 */
export async function deleteApiKey(): Promise<void> {
  await apiClient.delete('/api/apikey');
}
