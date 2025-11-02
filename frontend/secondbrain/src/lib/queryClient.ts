import { QueryClient } from '@tanstack/react-query';

/**
 * TanStack Query 클라이언트 설정
 * - staleTime: 데이터가 신선한 상태로 유지되는 시간
 * - gcTime: 사용하지 않는 캐시 데이터가 메모리에 유지되는 시간
 * - retry: 실패 시 재시도 횟수
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60 * 1000, // 1분
      gcTime: 5 * 60 * 1000, // 5분 (구 cacheTime)
      retry: 1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
});
