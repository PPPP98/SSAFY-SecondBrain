import { apiClient } from '@/api/client';
import type { NoteRequest, NoteResponse, NoteUpdateRequest } from '@/shared/types/note.types';

const API_BASE_URL = '/api/notes';

/**
 * API 응답 래퍼 타입
 * - 백엔드 BaseResponse와 일치
 */
interface ApiResponse<Result> {
  success: boolean;
  code: number;
  message: string;
  data: Result;
}

/**
 * TanStack Query용 QueryKey 팩토리
 * - 일관된 쿼리 키 관리
 * - 타입 안전성 보장
 */
export const noteQueries = {
  all: ['notes'] as const,
  lists: () => [...noteQueries.all, 'list'] as const,
  list: (filters?: string) => [...noteQueries.lists(), { filters }] as const,
  details: () => [...noteQueries.all, 'detail'] as const,
  detail: (id: number) => [...noteQueries.details(), id] as const,
};

/**
 * 노트 생성 (POST /api/notes)
 *
 * 검증:
 * - title, content 필수 (빈 값 불가)
 *
 * @param data - 노트 생성 요청 데이터
 * @returns 생성된 노트 정보
 * @throws Error - 검증 실패 또는 생성 실패
 */
export async function createNote(data: NoteRequest): Promise<NoteResponse> {
  const response = await apiClient.post<ApiResponse<NoteResponse>>(API_BASE_URL, data);
  return response.data.data;
}

/**
 * 노트 조회 (GET /api/notes/{id})
 *
 * @param id - 노트 ID
 * @returns 노트 정보
 * @throws Error - 노트 없음 (404) 또는 권한 없음 (403)
 */
export async function getNote(id: number): Promise<NoteResponse> {
  const response = await apiClient.get<ApiResponse<NoteResponse>>(`${API_BASE_URL}/${id}`);
  return response.data.data;
}

/**
 * 노트 수정 (PUT /api/notes/{id})
 *
 * @param id - 노트 ID
 * @param data - 수정 데이터 (title, content 선택)
 * @returns 수정된 노트 정보
 * @throws Error - 노트 없음 (404) 또는 권한 없음 (403)
 */
export async function updateNote(id: number, data: NoteUpdateRequest): Promise<NoteResponse> {
  const response = await apiClient.put<ApiResponse<NoteResponse>>(`${API_BASE_URL}/${id}`, data);
  return response.data.data;
}

/**
 * 노트 삭제 (DELETE /api/notes/{id})
 *
 * @param id - 노트 ID
 * @throws Error - 노트 없음 (404) 또는 권한 없음 (403)
 */
export async function deleteNote(id: number): Promise<void> {
  await apiClient.delete(`${API_BASE_URL}/${id}`);
}

/**
 * 노트 다중 삭제 (DELETE /api/notes)
 *
 * @param noteIds - 삭제할 노트 ID 배열
 * @throws Error - 노트 없음 (404) 또는 권한 없음 (403)
 */
export async function deleteNotes(noteIds: number[]): Promise<void> {
  await apiClient.delete(API_BASE_URL, {
    data: { noteIds },
  });
}
