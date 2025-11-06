import { ToggleSwitch } from '@/shared/components/ToggleSwitch/ToggleSwitch';
import { useAuthStore } from '@/stores/authStore';
import { useReminderToggle } from '@/features/reminder/hooks/useReminderToggle';

/**
 * 리마인더 알림 설정 토글 메뉴 아이템
 * - UserProfileMenu에서 사용
 * - Hybrid 패턴: Zustand + TanStack Query 함께 사용
 * - 백엔드 POST /api/users/reminder API 연동
 * - Optimistic UI 패턴 적용
 * - 로딩 상태 자동 처리
 */
export function ReminderToggleMenuItem() {
  const { user } = useAuthStore();
  const { toggle, isLoading } = useReminderToggle();

  if (!user) return null;

  return (
    <div
      role="menuitem"
      tabIndex={0}
      className="flex w-full items-center justify-between rounded px-4 py-2.5 text-left text-sm text-white transition-colors duration-150 ease-in-out hover:bg-white/10 focus:outline-none focus:ring-2 focus:ring-white/20 motion-reduce:transition-none"
    >
      <span>전체 알림 설정</span>
      <ToggleSwitch checked={user.setAlarm} onChange={toggle} disabled={isLoading} />
    </div>
  );
}
