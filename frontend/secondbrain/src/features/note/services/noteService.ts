import { apiClient } from '@/api/client';
import type { NoteGetRequest, NoteGetResponse } from '@/features/note/types/note';

const NOTE_ENDPOINT = '/api/notes';

export const noteAPI = {
  getNote: async (requestData: NoteGetRequest): Promise<NoteGetResponse> => {
    const response = await apiClient.get<NoteGetResponse>(`${NOTE_ENDPOINT}/${requestData.noteId}`);
    return response.data;
  },
};
