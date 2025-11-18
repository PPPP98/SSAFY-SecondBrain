import { env } from '@/config/env';
import browser from 'webextension-polyfill';
import type { NoteDetail } from '@/types/noteSearch';
import type { NoteSearchApiResponse } from '@/types/note';

/**
 * chrome.storage에서 Access Token 가져오기
 */
async function getAccessToken(): Promise<string | null> {
  const result = await browser.storage.local.get(['access_token']);
  return result.access_token as string | null;
}

/**
 * Elasticsearch 검색
 * GET /api/notes/search
 *
 * @param keyword - 검색 키워드
 * @returns NoteSearchApiResponse (노트 목록)
 * @throws Error (NO_TOKEN, API_ERROR)
 */
export async function searchNotes(keyword: string): Promise<NoteSearchApiResponse['data']> {
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
    throw new Error(`SEARCH_ERROR: ${response.status} ${response.statusText}`);
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
