import { useQuery } from '@tanstack/react-query';
import { searchAPI } from '@/features/main/services/searchService';

export function useRecentNotes() {
  return useQuery({
    queryKey: ['notes', 'recent'],
    queryFn: async () => {
      const response = await searchAPI.getRecentNote();
      return response.data;
    },
  });
}
