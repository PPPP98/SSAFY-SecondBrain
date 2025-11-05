import { useEffect } from 'react';
import { useModal } from '@/shared/hooks/useModal';
import type { DropdownProps } from '@/shared/components/Dropdown/Dropdown.types';

const positionClasses = {
  'bottom-right': 'right-0 top-full mt-2',
  'bottom-left': 'left-0 top-full mt-2',
  'top-right': 'right-0 bottom-full mb-2',
  'top-left': 'left-0 bottom-full mb-2',
};

/**
 * 드롭다운 컴포넌트
 * - 작은 드롭다운 메뉴용
 * - 절대 위치 기반
 * - 페이드 + translateY 애니메이션
 * - 키보드 네비게이션 지원
 */
export function Dropdown({
  isOpen,
  onClose,
  children,
  className = '',
  position = 'bottom-right',
  closeOnOutsideClick = true,
  closeOnEscape = true,
  enableKeyboardNav = true,
}: DropdownProps) {
  const { containerRef, contentRef } = useModal({
    isOpen,
    onClose,
    closeOnOutsideClick,
    closeOnEscape,
  });

  // 키보드 네비게이션
  useEffect(() => {
    if (!isOpen || !enableKeyboardNav) return;

    const contentElement = contentRef.current;
    if (!contentElement) return;

    // 메뉴 열릴 때 첫 번째 menuitem에 포커스
    const menuItems = contentElement.querySelectorAll<HTMLElement>('[role="menuitem"]');
    if (menuItems.length > 0) {
      menuItems[0].focus();
    }

    function handleKeyDown(event: KeyboardEvent) {
      const activeElement = document.activeElement as HTMLElement;
      const currentIndex = Array.from(menuItems).indexOf(activeElement);

      switch (event.key) {
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
    }

    contentElement.addEventListener('keydown', handleKeyDown);

    return () => {
      contentElement.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, enableKeyboardNav, contentRef]);

  return (
    <div
      ref={containerRef}
      className={`absolute z-[60] ${positionClasses[position]} transition-all duration-200 ease-out motion-reduce:transition-none ${
        isOpen
          ? 'pointer-events-auto translate-y-0 scale-100 opacity-100'
          : 'pointer-events-none -translate-y-2 scale-95 opacity-0'
      } ${className}`}
    >
      <div ref={contentRef}>{children}</div>
    </div>
  );
}
