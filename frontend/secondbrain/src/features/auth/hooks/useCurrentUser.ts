import { useQuery } from '@tanstack/react-query';

import { useAuthStore } from '@/stores/authStore';
import { getCurrentUser } from '@/features/auth/services/userService';

/**
 * 현재 로그인된 사용자 정보를 조회하는 Query 훅
 * - Access Token이 있을 때만 실행
 * - 5분간 캐시 유지
 *
 * @returns useQuery result with user data, isLoading, isError
 */
export function useCurrentUser() {
  const { accessToken } = useAuthStore();

  return useQuery({
    queryKey: ['user', 'me'],
    queryFn: () => getCurrentUser(),
    enabled: !!accessToken, // Access Token이 있을 때만 실행
    staleTime: 5 * 60 * 1000, // 5분
  });
}
