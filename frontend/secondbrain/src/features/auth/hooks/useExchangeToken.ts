import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';

import { useAuthStore } from '@/stores/authStore';
import { exchangeToken } from '@/features/auth/services/authService';
import { getCurrentUser } from '@/features/auth/services/userService';

/**
 * Authorization Code를 JWT 토큰으로 교환하는 Mutation 훅
 * - code 교환 후 Access Token 저장
 * - 사용자 정보 조회 후 메인으로 이동
 */
export function useExchangeToken() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { setAccessToken, setUser } = useAuthStore();

  return useMutation({
    mutationFn: (code: string) => exchangeToken(code),
    onSuccess: async (response) => {
      if (response.success && response.data) {
        // Access Token 저장
        setAccessToken(response.data.accessToken);

        // 사용자 정보 조회
        try {
          const userInfo = await getCurrentUser();
          setUser(userInfo);

          // 메인으로 이동
          void navigate({ to: '/main' });
        } catch (error) {
          console.error('Failed to fetch user info:', error);
          void navigate({ to: '/', search: { error: 'user_fetch_failed' } });
        }
      }
    },
    onError: (error) => {
      console.error('Token exchange failed:', error);

      // queryClient.clear() 대신 auth 관련 쿼리만 선택적으로 무효화
      void queryClient.invalidateQueries({ queryKey: ['session'] });
      void queryClient.invalidateQueries({ queryKey: ['user'] });

      // URL 파라미터 대신 랜딩페이지로 바로 이동
      // 에러 메시지는 필요 시 전역 상태나 토스트로 처리
      void navigate({ to: '/' });
    },
  });
}
