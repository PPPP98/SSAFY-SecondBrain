/**
 * API Key 응답 (Backend BaseResponse<ApiKeyResponse>)
 */
export interface ApiKeyResponse {
  apiKey: string;
}

/**
 * UserProfileMenu 뷰 상태
 */
export type UserProfileView = 'menu' | 'apikey-management';
