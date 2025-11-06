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
      // 서버 사이드 페이지네이션
      const { currentPage, totalPages } = lastPage;

      // 다음 페이지가 있는지 확인
      if (currentPage < totalPages - 1) {
        return currentPage + 1;
      }
      return undefined;
    },
    // 검색어가 있을 때만 실행
    enabled: keyword.trim().length > 0,
  });
}
