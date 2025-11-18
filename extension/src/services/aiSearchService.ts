import { env } from '@/config/env';
import browser from 'webextension-polyfill';
import type { SearchResponse, NoteDetail } from '@/types/aiSearch';
import type { NoteSearchApiResponse } from '@/types/note';

/**
 * chrome.storage에서 Access Token 가져오기
 */
async function getAccessToken(): Promise<string | null> {
  const result = await browser.storage.local.get(['access_token']);
  return result.access_token as string | null;
}

/**
 * chrome.storage에서 User ID 가져오기
 * user 객체에서 id 추출
 */
async function getUserId(): Promise<number | null> {
  const result = await browser.storage.local.get(['user']);
  if (result.user && typeof result.user === 'object' && 'id' in result.user) {
    return (result.user as { id: number }).id;
  }
  return null;
}

/**
 * AI Agent 검색
 * GET /ai/api/v1/agents/search
 *
 * @param keyword - 검색 키워드
 * @returns SearchResponse (AI 답변 + 관련 문서)
 * @throws Error (NO_TOKEN, API_ERROR)
 */
export async function searchWithAI(keyword: string): Promise<SearchResponse> {
  const token = await getAccessToken();
  const userId = await getUserId();

  if (!token) {
    throw new Error('NO_TOKEN');
  }

  if (!userId) {
    throw new Error('NO_USER_ID');
  }

  const url = `${env.kgApiBaseUrl}/ai/api/v1/agents/search?query=${encodeURIComponent(keyword)}`;

  const response = await fetch(url, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      'X-User-ID': userId.toString(),
    },
  });

  if (!response.ok) {
    throw new Error(`AI_SEARCH_ERROR: ${response.status} ${response.statusText}`);
  }

  return (await response.json()) as SearchResponse;
}

/**
 * Elasticsearch 검색
 * GET /api/notes/search
 *
 * @param keyword - 검색 키워드
 * @returns NoteSearchApiResponse (노트 목록)
 * @throws Error (NO_TOKEN, API_ERROR)
 */
export async function searchWithElasticsearch(
  keyword: string,
): Promise<NoteSearchApiResponse['data']> {
  const token = await getAccessToken();

  if (!token) {
    throw new Error('NO_TOKEN');
  }

  const response = await fetch(
    `${env.apiBaseUrl}/api/notes/search?keyword=${encodeURIComponent(keyword)}&page=0&size=5`,
    {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    },
  );

  if (!response.ok) {
    throw new Error(`ES_SEARCH_ERROR: ${response.status} ${response.statusText}`);
  }

  const data = (await response.json()) as NoteSearchApiResponse;
  return data.data;
}

/**
 * 노트 상세 조회
 * GET /api/notes/{noteId}
 *
 * @param noteId - 노트 ID
 * @returns NoteDetail (노트 전체 내용, 마크다운)
 * @throws Error (NO_TOKEN, API_ERROR)
 */
export async function getNoteDetail(noteId: number): Promise<NoteDetail> {
  const token = await getAccessToken();

  if (!token) {
    throw new Error('NO_TOKEN');
  }

  const response = await fetch(`${env.apiBaseUrl}/api/notes/${noteId}`, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`NOTE_DETAIL_ERROR: ${response.status} ${response.statusText}`);
  }

  const data = (await response.json()) as { data: NoteDetail };
  return data.data;
}
