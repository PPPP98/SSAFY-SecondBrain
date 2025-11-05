import { useState, useEffect, useRef } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { GlassElement } from '@/shared/components/GlassElement/GlassElement';
import { UserAvatar } from '@/features/auth/components/UserAvatar';
import { UserProfileMenu } from '@/features/auth/components/UserProfileMenu';
import UserIcon from '@/shared/components/icon/User.svg?react';

/**
 * 사용자 프로필 버튼 컴포넌트
 * - 사용자 프로필 이미지 또는 기본 아이콘 표시
 * - 클릭 시 프로필 메뉴 토글
 * - 외부 클릭 및 Escape 키로 메뉴 닫기
 * - 포커스 트랩 및 키보드 네비게이션 지원
 * - 접근성 속성 포함
 */

export function UserProfileButton() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const { user } = useAuthStore();
  const containerRef = useRef<HTMLDivElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  const toggleMenu = () => {
    setIsMenuOpen((prev) => !prev);
  };

  const closeMenu = () => {
    setIsMenuOpen(false);
  };

  // 외부 클릭 감지로 메뉴 닫기
  useEffect(() => {
    if (!isMenuOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        closeMenu();
      }
    };

    // 약간의 지연을 두어 토글 클릭과 외부 클릭이 충돌하지 않도록 함
    const timeoutId = setTimeout(() => {
      document.addEventListener('click', handleClickOutside);
    }, 0);

    return () => {
      clearTimeout(timeoutId);
      document.removeEventListener('click', handleClickOutside);
    };
  }, [isMenuOpen]);

  // 포커스 트랩 및 키보드 네비게이션
  useEffect(() => {
    if (!isMenuOpen) return;

    const menuElement = menuRef.current;
    if (!menuElement) return;

    // 메뉴 열릴 때 첫 번째 menuitem에 포커스
    const menuItems = menuElement.querySelectorAll<HTMLElement>('[role="menuitem"]');
    if (menuItems.length > 0) {
      menuItems[0].focus();
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      const activeElement = document.activeElement as HTMLElement;
      const currentIndex = Array.from(menuItems).indexOf(activeElement);

      switch (event.key) {
        case 'Escape': {
          event.preventDefault();
          closeMenu();
          // 버튼으로 포커스 복귀
          const button = containerRef.current?.querySelector('button');
          button?.focus();
          break;
        }

        case 'ArrowDown':
          event.preventDefault();
          if (menuItems.length > 0) {
            const nextIndex = currentIndex < menuItems.length - 1 ? currentIndex + 1 : 0;
            menuItems[nextIndex].focus();
          }
          break;

        case 'ArrowUp':
          event.preventDefault();
          if (menuItems.length > 0) {
            const prevIndex = currentIndex > 0 ? currentIndex - 1 : menuItems.length - 1;
            menuItems[prevIndex].focus();
          }
          break;

        case 'Home':
          event.preventDefault();
          if (menuItems.length > 0) {
            menuItems[0].focus();
          }
          break;

        case 'End':
          event.preventDefault();
          if (menuItems.length > 0) {
            menuItems[menuItems.length - 1].focus();
          }
          break;

        case 'Tab':
          event.preventDefault();
          // Tab으로 순환
          if (menuItems.length > 0) {
            const nextIndex = event.shiftKey
              ? currentIndex > 0
                ? currentIndex - 1
                : menuItems.length - 1
              : currentIndex < menuItems.length - 1
                ? currentIndex + 1
                : 0;
            menuItems[nextIndex].focus();
          }
          break;
      }
    };

    menuElement.addEventListener('keydown', handleKeyDown);

    return () => {
      menuElement.removeEventListener('keydown', handleKeyDown);
    };
  }, [isMenuOpen]);

  return (
    <div ref={containerRef} className="relative">
      <GlassElement
        as="button"
        icon={
          <UserAvatar
            src={user?.picture}
            alt={user?.name || '사용자'}
            size="md"
            fallbackIcon={<UserIcon className="size-6" />}
          />
        }
        onClick={toggleMenu}
        aria-label="사용자 프로필 메뉴"
        aria-expanded={isMenuOpen}
        aria-haspopup="true"
      />

      <UserProfileMenu isOpen={isMenuOpen} onClose={closeMenu} menuRef={menuRef} />
    </div>
  );
}
