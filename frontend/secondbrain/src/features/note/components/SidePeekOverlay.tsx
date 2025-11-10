import { type ReactNode } from 'react';

interface SidePeekOverlayProps {
  isOpen: boolean;
  onClose: () => void;
  children: ReactNode;
}

/**
 * Side Peek 공통 오버레이 컨테이너
 * - 배경 오버레이 (클릭 시 닫기)
 * - 슬라이드 애니메이션 (왼쪽→오른쪽)
 * - TailwindCSS transform 사용
 */
export function SidePeekOverlay({ isOpen, onClose, children }: SidePeekOverlayProps) {
  return (
    <>
      {/* 배경 오버레이 */}
      <div
        className={`fixed inset-0 z-[100] bg-transparent transition-opacity duration-300 ${
          isOpen ? 'pointer-events-auto opacity-100' : 'pointer-events-none opacity-0'
        }`}
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Side Peek 패널 */}
      <div
        className={`fixed right-0 top-0 z-[110] size-full backdrop-blur-xl transition-all duration-500 ease-out md:w-3/4 lg:w-2/3 xl:w-1/2 2xl:w-2/5 ${
          isOpen
            ? 'pointer-events-auto translate-x-0 opacity-100'
            : 'pointer-events-none translate-x-full opacity-0'
        }`}
        role="dialog"
        aria-modal="true"
      >
        {children}
      </div>
    </>
  );
}
