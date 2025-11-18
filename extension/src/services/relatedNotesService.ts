import { env } from '@/config/env';
import browser from 'webextension-polyfill';
import type { RelatedNotesResponse } from '@/types/note';
import type { UserInfo } from '@/types/auth';

/**
 * 인증 헤더 정보 조회
 * chrome.storage에서 access_token과 user 정보를 가져옴
 */
async function getAuthHeaders(): Promise<{
  token: string;
  userId: number;
}> {
  const result = await browser.storage.local.get(['access_token', 'user']);
  const token = result.access_token as string | null;
  const user = result.user as UserInfo | null;

  if (!token) throw new Error('NO_TOKEN');
  if (!user?.id) throw new Error('NO_USER_ID');

  return { token, userId: user.id };
}

/**
 * 관련 노트 조회 결과 타입
 */
export interface RelatedNoteItem {
  id: number;
  title: string;
  distance: number;
}

/**
 * Knowledge Graph Service에서 관련 노트 조회
 * GET /ai/api/v1/graph/neighbors/{note_id}
 *
 * @param noteId - 중심 노트 ID
 * @param depth - 그래프 탐색 깊이 (1-3, 기본값: 1)
 * @returns 거리순으로 정렬된 관련 노트 배열
 * @throws Error (NO_TOKEN, NO_USER_ID, RELATED_NOTES_ERROR)
 */
export async function getRelatedNotes(
  noteId: number,
  depth: number = 1,
): Promise<RelatedNoteItem[]> {
  const { token, userId } = await getAuthHeaders();

  // depth 범위 제한 (1-3)
  const validDepth = Math.min(Math.max(depth, 1), 3);

  const response = await fetch(
    `${env.kgApiBaseUrl}/ai/api/v1/graph/neighbors/${noteId}?depth=${validDepth}`,
    {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        'X-User-ID': userId.toString(),
        'Content-Type': 'application/json',
      },
    },
  );

  if (!response.ok) {
    // 404는 관련 노트가 없는 경우 - 빈 배열 반환
    if (response.status === 404) {
      return [];
    }
    throw new Error(`RELATED_NOTES_ERROR: ${response.status}`);
  }

  const data = (await response.json()) as RelatedNotesResponse;

  // 데이터 변환 및 거리순 정렬
  return data.neighbors
    .map((neighbor) => ({
      id: neighbor.neighbor_id,
      title: neighbor.neighbor_title,
      distance: neighbor.distance,
    }))
    .sort((a, b) => a.distance - b.distance);
}
