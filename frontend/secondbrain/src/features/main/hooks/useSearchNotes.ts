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
      // currentPage가 totalPages - 1보다 작으면 다음 페이지가 있음
      const hasNextPage = lastPage.currentPage < lastPage.totalPages - 1;
      if (hasNextPage) {
        return lastPage.currentPage + 1;
      }
      return undefined;
    },
    enabled: keyword.trim().length > 0,
  });
}
