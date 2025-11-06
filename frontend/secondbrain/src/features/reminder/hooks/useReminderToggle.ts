import { useMutation, useQueryClient } from '@tanstack/react-query';

import type { UserInfo } from '@/features/auth/types/auth';
import { toggleReminder } from '@/features/reminder/services/reminderService';
import { useAuthStore } from '@/stores/authStore';

/**
 * 리마인더 알림 설정 토글 커스텀 훅
 * - TanStack Query useMutation 사용
 * - Hybrid 패턴: Query Cache + Zustand 동시 업데이트
 * - Optimistic UI 패턴 적용 (즉시 UI 반영)
 * - 에러 시 자동 롤백
 * - Context7 공식 권장 패턴 준수
 *
 * @returns {object} toggle 함수, 로딩 상태, 에러 정보
 */
export function useReminderToggle() {
  const queryClient = useQueryClient();
  const { user: zustandUser, setUser } = useAuthStore();

  const mutation = useMutation({
    mutationFn: toggleReminder,

    // 낙관적 업데이트 (Optimistic UI) - 공식 권장 패턴
    onMutate: async () => {
      // 진행 중인 user 쿼리 취소 (오래된 데이터가 낙관적 업데이트를 덮어쓰지 않도록)
      await queryClient.cancelQueries({ queryKey: ['user', 'me'] });

      // Query Cache에서 이전 상태 스냅샷 저장
      const previousQueryUser = queryClient.getQueryData<UserInfo>(['user', 'me']);
      const previousZustandUser = zustandUser ? { ...zustandUser } : null;

      // Query Cache를 낙관적으로 업데이트
      queryClient.setQueryData<UserInfo>(['user', 'me'], (old) => {
        if (!old) return old;
        return { ...old, setAlarm: !old.setAlarm };
      });

      // Zustand도 동기화 (전역 접근용)
      if (zustandUser) {
        setUser({ ...zustandUser, setAlarm: !zustandUser.setAlarm });
      }

      return { previousQueryUser, previousZustandUser };
    },

    // 실패 시 롤백 (Query Cache + Zustand 둘 다)
    onError: (error, _variables, context) => {
      if (context?.previousQueryUser) {
        queryClient.setQueryData(['user', 'me'], context.previousQueryUser);
      }
      if (context?.previousZustandUser) {
        setUser(context.previousZustandUser);
      }
      console.error('Failed to toggle reminder:', error);
    },

    // 성공/실패 후 항상 재검증
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: ['user', 'me'] });
    },
  });

  return {
    toggle: () => mutation.mutate(),
    isLoading: mutation.isPending,
    error: mutation.error,
  };
}
