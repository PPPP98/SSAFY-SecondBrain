import { useInfiniteQuery } from '@tanstack/react-query';
import { searchAPI } from '@/features/main/services/searchService';
import type { SearchNoteData } from '@/features/main/types/search';

const PAGE_SIZE = 10;

interface UseSearchNotesParams {
  keyword: string;
}

export function useSearchNotes({ keyword }: UseSearchNotesParams) {
  return useInfiniteQuery<SearchNoteData>({
    queryKey: ['notes', 'search', keyword],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await searchAPI.getSearchNote({
        keyword,
        page: pageParam as number,
        size: PAGE_SIZE,
      });
      return response.data;
    },
    initialPageParam: 0,
    getNextPageParam: (lastPage) => {
      if (lastPage.hasNext) {
        const currentPageParam =
          (lastPage as SearchNoteData & { pageParam?: number }).pageParam ?? 0;
        return currentPageParam + 1;
      }
      return undefined;
    },
    enabled: keyword.trim().length > 0,
  });
}
