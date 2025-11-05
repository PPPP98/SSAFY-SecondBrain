import { useQuery } from '@tanstack/react-query';
import { graphAPI } from '@/features/main/services/graphService';

export function useGraphVisualization() {
  return useQuery({
    queryKey: ['graphs', 'visualization'],
    queryFn: () => graphAPI.getGraphVisualization(),
    staleTime: 2 * 60 * 1000, // 2분 캐시
    retry: 2, // 실패 시 2번 재시도
  });
}
