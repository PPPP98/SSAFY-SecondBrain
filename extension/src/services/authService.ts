import { env } from '@/config/env';
import type { BaseResponse } from '@/types/api';
import type { TokenResponse } from '@/types/auth';

/**
 * Chrome Extension용 Auth Service (fetch 기반)
 * - Background Service Worker에서 사용
 * - chrome.storage와 통합
 */

/**
 * chrome.storage에서 Access Token 가져오기
 */
async function getAccessToken(): Promise<string | null> {
  const { default: browser } = await import('webextension-polyfill');
  const result = await browser.storage.local.get(['access_token']);
  return result.access_token as string | null;
}

/**
 * POST /api/auth/token
 * Authorization Code를 JWT 토큰으로 교환
 * @param code - OAuth2 Authorization Code
 * @returns BaseResponse<TokenResponse>
 */
export async function exchangeToken(code: string): Promise<BaseResponse<TokenResponse>> {
  const response = await fetch(`${env.apiBaseUrl}/api/auth/token?code=${code}`, {
    method: 'POST',
    credentials: 'include', // 쿠키 전송 허용
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`Token exchange failed: ${response.status} ${response.statusText}`);
  }

  return response.json() as Promise<BaseResponse<TokenResponse>>;
}

/**
 * POST /api/auth/refresh
 * Refresh Token으로 새 Access Token 발급
 * @returns BaseResponse<TokenResponse> 또는 null (401 에러 시)
 */
export async function refreshToken(): Promise<BaseResponse<TokenResponse> | null> {
  try {
    const response = await fetch(`${env.apiBaseUrl}/api/auth/refresh`, {
      method: 'POST',
      credentials: 'include', // Refresh Token 쿠키 전송
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // 401 Unauthorized는 로그아웃 상태로 처리 (에러를 throw하지 않음)
    if (response.status === 401) {
      console.info('Refresh token expired or invalid - user needs to re-authenticate');
      return null;
    }

    if (!response.ok) {
      throw new Error(`Token refresh failed: ${response.status} ${response.statusText}`);
    }

    return response.json() as Promise<BaseResponse<TokenResponse>>;
  } catch (error) {
    console.error('Token refresh error:', error);
    throw error;
  }
}

/**
 * POST /api/auth/logout
 * 로그아웃 처리 (Refresh Token 무효화)
 * @returns BaseResponse<null>
 */
export async function logout(): Promise<BaseResponse<null>> {
  const token = await getAccessToken();

  const response = await fetch(`${env.apiBaseUrl}/api/auth/logout`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
    },
  });

  if (!response.ok) {
    throw new Error(`Logout failed: ${response.status} ${response.statusText}`);
  }

  return response.json() as Promise<BaseResponse<null>>;
}
