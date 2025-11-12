import { apiClient } from '@/api/client';
import type {
  NoteDeleteRequest,
  NoteDeleteResponse,
  NoteGetRequest,
  NoteGetResponse,
} from '@/features/note/types/note';

const NOTE_ENDPOINT = '/api/notes';

export const noteAPI = {
  getNote: async (requestData: NoteGetRequest): Promise<NoteGetResponse> => {
    const response = await apiClient.get<NoteGetResponse>(`${NOTE_ENDPOINT}/${requestData.noteId}`);
    return response.data;
  },
  deleteNotes: async (requestData: NoteDeleteRequest): Promise<NoteDeleteResponse> => {
    const response = await apiClient.delete<NoteDeleteResponse>(NOTE_ENDPOINT, {
      data: requestData,
    });
    return response.data;
  },
};
