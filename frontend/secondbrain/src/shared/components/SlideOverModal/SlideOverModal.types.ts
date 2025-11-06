import type { ReactNode } from 'react';

export interface SlideOverModalProps {
  isOpen: boolean;
  onClose: () => void;
  children: ReactNode;
  /** 모달 너비 (기본: 50%) */
  width?: string;
  /** 슬라이드 방향 (기본: right) */
  direction?: 'left' | 'right';
  /** Escape 키로 닫기 활성화 여부 */
  closeOnEscape?: boolean;
}
