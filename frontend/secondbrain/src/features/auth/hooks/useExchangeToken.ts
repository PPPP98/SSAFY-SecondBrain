import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';

import { useAuthStore } from '@/stores/authStore';
import { exchangeToken } from '@/features/auth/services/authService';
import { getCurrentUser } from '@/features/auth/services/userService';

/**
 * Authorization Code를 JWT 토큰으로 교환하는 Mutation 훅
 * - code 교환 후 Access Token 저장
 * - 사용자 정보 조회 후 대시보드로 이동
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

          // 대시보드로 이동
          void navigate({ to: '/dashboard' });
        } catch (error) {
          console.error('Failed to fetch user info:', error);
          void navigate({ to: '/', search: { error: 'user_fetch_failed' } });
        }
      }
    },
    onError: (error) => {
      console.error('Token exchange failed:', error);
      queryClient.clear();
      void navigate({ to: '/', search: { error: 'token_exchange_failed' } });
    },
  });
}
