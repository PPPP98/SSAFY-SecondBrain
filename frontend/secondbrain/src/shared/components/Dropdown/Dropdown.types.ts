import type { ReactNode } from 'react';

export interface DropdownProps {
  isOpen: boolean;
  onClose: () => void;
  children: ReactNode;
  className?: string;
  /** 드롭다운 위치 (기본: 버튼 하단 우측) */
  position?: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left';
  /** 외부 클릭 시 닫기 활성화 여부 */
  closeOnOutsideClick?: boolean;
  /** Escape 키로 닫기 활성화 여부 */
  closeOnEscape?: boolean;
  /** 키보드 네비게이션 활성화 여부 */
  enableKeyboardNav?: boolean;
}
