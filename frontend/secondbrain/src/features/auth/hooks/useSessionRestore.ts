import { useQuery } from '@tanstack/react-query';

import { useAuthStore } from '@/stores/authStore';
import { refreshToken } from '@/features/auth/services/authService';
import { getCurrentUser } from '@/features/auth/services/userService';

/**
 * 페이지 로드 시 Refresh Token으로 세션을 자동 복원하는 Query 훅
 * - Refresh Token으로 새 Access Token 발급 시도
 * - 성공 시 사용자 정보 자동 조회 및 저장
 * - 실패 시 조용히 무시 (로그인 전 상태)
 * - TanStack Query를 사용하여 로딩/에러 상태 자동 관리
 *
 * @returns useQuery result with isLoading, isError, data
 */
export function useSessionRestore() {
  const { setAccessToken, setUser } = useAuthStore();

  return useQuery({
    queryKey: ['session', 'restore'],
    queryFn: async () => {
      // 1. Refresh Token으로 새 Access Token 발급
      const response = await refreshToken();

      if (response.success && response.data) {
        // 2. Access Token 저장
        setAccessToken(response.data.accessToken);

        // 3. 사용자 정보 조회 (BaseResponse 없이 직접 반환)
        const userInfo = await getCurrentUser();
        setUser(userInfo);

        return userInfo;
      }

      throw new Error('No active session');
    },
    retry: false, // 세션 복원 실패 시 재시도 안 함
    staleTime: Infinity, // 성공 시 캐시 유지, 실패 시에만 재시도
    gcTime: 5 * 60 * 1000, // 5분 후 에러 캐시 삭제
    refetchOnWindowFocus: false, // 포커스 시 재실행 방지
    refetchOnMount: true, // 마운트 시에만 실행
    refetchOnReconnect: false, // 재연결 시 실행 안 함
  });
}
