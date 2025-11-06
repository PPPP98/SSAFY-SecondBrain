import { useAuthStore } from '@/stores/authStore';
import { GlassElement } from '@/shared/components/GlassElement/GlassElement';
import { LogoutButton } from '@/features/auth/components/LogoutButton';
import { ReminderToggleMenuItem } from '@/features/reminder/components/ReminderToggleMenuItem';
import { Dropdown } from '@/shared/components/Dropdown/Dropdown';
import LogoutIcon from '@/shared/components/icon/Logout.svg?react';

/**
 * 사용자 프로필 드롭다운 메뉴
 * - 사용자 정보 표시
 * - 로그아웃 버튼 포함
 * - GlassElement 기반 스타일
 * - Dropdown 컴포넌트 사용
 */

interface UserProfileMenuProps {
  isOpen: boolean;
  onClose: () => void;
}

export function UserProfileMenu({ isOpen, onClose }: UserProfileMenuProps) {
  const { user } = useAuthStore();

  if (!user) return null;

  return (
    <Dropdown isOpen={isOpen} onClose={onClose} position="bottom-right">
      <GlassElement as="div" className="w-max min-w-[200px]">
        <div role="menu" className="w-full space-y-1 p-2">
          {/* 사용자 정보 */}
          <div className="px-4 py-2">
            <p className="text-sm font-medium text-white">{user.name}</p>
            <p className="text-xs text-white/70">{user.email}</p>
          </div>

          {/* 구분선 */}
          <hr className="border-white/20" />

          {/* 전체 알림 설정 */}
          <ReminderToggleMenuItem />

          {/* 리마인더 관리 */}
          <button
            role="menuitem"
            className="flex w-full items-center justify-between rounded px-4 py-2.5 text-left text-sm text-white transition-colors duration-150 ease-in-out hover:bg-white/10 focus:outline-none focus:ring-2 focus:ring-white/20 motion-reduce:transition-none"
            onClick={() => {
              // 향후: 리마인더 관리 페이지로 이동
            }}
          >
            <span>리마인더 관리</span>
          </button>

          {/* 구분선 */}
          <hr className="border-white/20" />

          {/* 로그아웃 버튼 */}
          <LogoutButton
            variant="menu-item"
            size="sm"
            icon={<LogoutIcon className="size-5" />}
            onLogoutStart={onClose}
          />
        </div>
      </GlassElement>
    </Dropdown>
  );
}
