import { useQuery } from '@tanstack/react-query';
import { searchAPI } from '@/features/main/services/searchService';
import type { RecentNote } from '@/features/main/types/search';

interface UseRecentNotesOptions<Select = RecentNote[]> {
  select?: (data: RecentNote[]) => Select;
}

export function useRecentNotes<Select = RecentNote[]>(options?: UseRecentNotesOptions<Select>) {
  return useQuery<RecentNote[], Error, Select>({
    queryKey: ['notes', 'recent'],
    queryFn: async () => {
      const response = await searchAPI.getRecentNote();
      return response.data;
    },
    ...options,
  });
}
