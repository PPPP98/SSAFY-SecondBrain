import apiClient from '@/api/client';
import type {
  RecentNoteResponse,
  SearchNoteRequest,
  SearchNoteResponse,
  SimilarNoteRequest,
  SimilarNoteResponse,
} from '@/features/main/types/search';

const SEARCH_ENDPOINT = '/api/notes';

export const searchAPI = {
  getSimilarNote: async (requestData: SimilarNoteRequest): Promise<SimilarNoteResponse> => {
    const response = await apiClient.get<SimilarNoteResponse>(
      `${SEARCH_ENDPOINT}/${requestData.noteId}/similar`,
      {
        params: {
          limit: requestData.limit,
        },
      },
    );
    return response.data;
  },

  getSearchNote: async (requestData: SearchNoteRequest): Promise<SearchNoteResponse> => {
    const response = await apiClient.get<SearchNoteResponse>(`${SEARCH_ENDPOINT}/search`, {
      params: requestData,
    });
    return response.data;
  },

  getRecentNote: async (): Promise<RecentNoteResponse> => {
    const response = await apiClient.get<RecentNoteResponse>(`${SEARCH_ENDPOINT}/recent`);
    return response.data;
  },
};
