import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';

import { useAuthStore } from '@/stores/authStore';
import { logout } from '@/features/auth/services/authService';

/**
 * 로그아웃을 처리하는 Mutation 훅
 * - 로그아웃 API 호출
 * - Zustand 스토어 초기화
 * - TanStack Query 캐시 초기화
 * - 랜딩페이지로 리다이렉트
 *
 * @returns useMutation result with mutate, isPending
 */
export function useLogout() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { clearAuth } = useAuthStore();

  return useMutation({
    mutationFn: () => logout(),
    onSuccess: (response) => {
      if (response.success) {
        // Zustand 스토어 초기화
        clearAuth();

        // TanStack Query 캐시 초기화
        queryClient.clear();

        // 랜딩페이지로 이동
        void navigate({ to: '/' });
      }
    },
    onError: (error) => {
      console.error('Logout failed:', error);
      // 에러가 발생해도 클라이언트 측 로그아웃은 진행
      clearAuth();
      queryClient.clear();
      void navigate({ to: '/' });
    },
  });
}
