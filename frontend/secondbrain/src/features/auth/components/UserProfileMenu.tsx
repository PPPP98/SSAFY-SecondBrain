import { useAuthStore } from '@/stores/authStore';
import { GlassElement } from '@/shared/components/GlassElement/GlassElement';
import { LogoutButton } from '@/features/auth/components/LogoutButton';
import { ToggleSwitch } from '@/shared/components/ToggleSwitch/ToggleSwitch';
import LogoutIcon from '@/shared/components/icon/Logout.svg?react';

/**
 * 사용자 프로필 드롭다운 메뉴
 * - 사용자 정보 표시
 * - 로그아웃 버튼 포함
 * - GlassElement 기반 스타일
 */

interface UserProfileMenuProps {
  isOpen: boolean;
  onClose: () => void;
  menuRef: React.RefObject<HTMLDivElement>;
}

export function UserProfileMenu({ isOpen, onClose, menuRef }: UserProfileMenuProps) {
  const { user, setUser } = useAuthStore();

  if (!user) return null;

  const handleNotificationToggle = (checked: boolean) => {
    setUser({ ...user, setAlarm: checked });
    // 향후: API 호출 추가
  };

  return (
    <div
      ref={menuRef}
      className={`absolute right-0 top-full z-[60] mt-2 transition-all duration-200 ease-out motion-reduce:transition-none ${
        isOpen
          ? 'pointer-events-auto translate-y-0 scale-100 opacity-100'
          : 'pointer-events-none -translate-y-2 scale-95 opacity-0'
      }`}
    >
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
          <button
            role="menuitem"
            className="flex w-full items-center justify-between rounded px-4 py-2.5 text-left text-sm text-white transition-colors duration-150 ease-in-out hover:bg-white/10 focus:outline-none focus:ring-2 focus:ring-white/20 motion-reduce:transition-none"
            onClick={(e) => {
              e.stopPropagation();
              handleNotificationToggle(!user.setAlarm);
            }}
          >
            <span>전체 알림 설정</span>
            <ToggleSwitch checked={user.setAlarm} onChange={handleNotificationToggle} />
          </button>

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
    </div>
  );
}
