import { env } from '@/config/env';
import type { UserInfo } from '@/types/auth';
import browser from 'webextension-polyfill';

/**
 * Chrome Extension용 User Service (fetch 기반)
 * - Background Service Worker에서 사용
 */

/**
 * chrome.storage에서 Access Token 가져오기
 */
async function getAccessToken(): Promise<string | null> {
  const result = await browser.storage.local.get(['access_token']);
  return result.access_token as string | null;
}

/**
 * GET /api/users/me
 * 현재 로그인된 사용자 정보 조회
 * ⚠️ 주의: 이 API는 BaseResponse 없이 UserInfo를 직접 반환
 * @returns UserInfo
 */
export async function getCurrentUser(): Promise<UserInfo> {
  const token = await getAccessToken();

  if (!token) {
    throw new Error('No access token available');
  }

  const response = await fetch(`${env.apiBaseUrl}/api/users/me`, {
    method: 'GET',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch user info: ${response.status} ${response.statusText}`);
  }

  return response.json() as Promise<UserInfo>;
}
