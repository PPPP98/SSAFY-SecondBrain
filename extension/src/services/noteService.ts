import { env } from '@/config/env';
import type { SavePageRequest, SavePageResponse, SavePageError } from '@/types/note';
import browser from 'webextension-polyfill';

/**
 * chrome.storage에서 Access Token 가져오기
 */
async function getAccessToken(): Promise<string | null> {
  const result = await browser.storage.local.get(['access_token']);
  return result.access_token as string | null;
}

/**
 * POST /ai/api/v1/agents/summarize
 * 페이지 URL(들)을 Knowledge Graph Service로 전송하여 노트 생성
 *
 * **Flow:**
 * 1. URL 크롤링 (trafilatura)
 * 2. LLM 요약 생성 (OpenAI)
 * 3. Spring Boot API 호출하여 노트 생성
 * 4. Neo4j 지식 그래프에 저장
 *
 * @param urls - 저장할 URL 배열 (단일 또는 다중 페이지)
 * @param token - JWT Access Token
 * @returns SavePageResponse
 * @throws SavePageError
 */
export async function saveCurrentPage(urls: string[], token: string): Promise<SavePageResponse> {
  try {
    const requestBody: SavePageRequest = {
      data: urls, // URL 배열로 전송
    };

    const response = await fetch(`${env.kgApiBaseUrl}/ai/api/v1/agents/summarize`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(requestBody),
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error('API Error Response:', errorText);

      const error: SavePageError = {
        error: 'API_ERROR',
        message: `Failed to save page: ${response.status} ${response.statusText}`,
        status: response.status,
      };
      // eslint-disable-next-line @typescript-eslint/only-throw-error
      throw error;
    }

    return (await response.json()) as SavePageResponse;
  } catch (error) {
    // 네트워크 오류 또는 기타 예외
    if (error && typeof error === 'object' && 'error' in error) {
      throw error; // SavePageError 재전달
    }

    const networkError: SavePageError = {
      error: 'NETWORK_ERROR',
      message: error instanceof Error ? error.message : 'Unknown error occurred',
    };
    // eslint-disable-next-line @typescript-eslint/only-throw-error
    throw networkError;
  }
}

/**
 * chrome.storage에서 토큰을 가져와 페이지 저장
 * Background Service Worker에서 사용
 *
 * @param urls - 저장할 URL 배열 (단일 또는 다중 페이지)
 * @returns SavePageResponse
 * @throws SavePageError (토큰 없음 또는 API 오류)
 */
export async function saveCurrentPageWithStoredToken(urls: string[]): Promise<SavePageResponse> {
  const token = await getAccessToken();

  if (!token) {
    const error: SavePageError = {
      error: 'NO_TOKEN',
      message: 'Access token not found. Please login first.',
    };
    // eslint-disable-next-line @typescript-eslint/only-throw-error
    throw error;
  }

  return saveCurrentPage(urls, token);
}
