import { type ReactNode, useState, useRef, useEffect, useCallback } from 'react';

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
 * - 크기 조절 가능 (왼쪽 border 드래그)
 */
export function SidePeekOverlay({ isOpen, onClose, children }: SidePeekOverlayProps) {
  // 패널 너비 (percentage)
  const [width, setWidth] = useState<number>(50);

  // 드래그 상태
  const isResizing = useRef(false);
  const startX = useRef(0);
  const startWidth = useRef(50);

  // 드래그 시작
  const handleMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    isResizing.current = true;
    startX.current = e.clientX;
    startWidth.current = width;
    document.body.style.userSelect = 'none';
  };

  // 드래그 중 크기 조절
  const handleMouseMove = useCallback((e: MouseEvent) => {
    if (!isResizing.current) return;

    const deltaX = startX.current - e.clientX; // 왼쪽으로 드래그하면 +
    const windowWidth = window.innerWidth;
    const newWidthPx = (startWidth.current / 100) * windowWidth + deltaX;
    const newWidthPercent = (newWidthPx / windowWidth) * 100;

    // 최소 30%, 최대 95%
    const clampedWidth = Math.min(Math.max(newWidthPercent, 30), 95);
    setWidth(clampedWidth);
  }, []);

  // 드래그 종료
  const handleMouseUp = useCallback(() => {
    isResizing.current = false;
    document.body.style.userSelect = '';
  }, []);

  // Document event listeners
  useEffect(() => {
    if (isResizing.current) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);

      return () => {
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
      };
    }
  }, [handleMouseMove, handleMouseUp]);

  // 화면 크기에 따른 초기 너비 설정
  useEffect(() => {
    const updateInitialWidth = () => {
      const windowWidth = window.innerWidth;
      if (windowWidth >= 1536)
        setWidth(40); // 2xl
      else if (windowWidth >= 1280)
        setWidth(50); // xl
      else if (windowWidth >= 1024)
        setWidth(66.67); // lg
      else if (windowWidth >= 768)
        setWidth(75); // md
      else setWidth(100); // mobile
    };

    updateInitialWidth();
    window.addEventListener('resize', updateInitialWidth);
    return () => window.removeEventListener('resize', updateInitialWidth);
  }, []);

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
        className={`fixed right-0 top-0 z-[110] h-full border-l-2 border-white/30 backdrop-blur-xl transition-all duration-500 ease-out ${
          isOpen
            ? 'pointer-events-auto translate-x-0 opacity-100'
            : 'pointer-events-none translate-x-full opacity-0'
        }`}
        style={{ width: `${width}%` }}
        role="dialog"
        aria-modal="true"
      >
        {/* Resize Handle */}
        <div
          className="absolute left-0 top-0 z-[120] h-full w-2 cursor-ew-resize transition-colors hover:bg-white/40 active:bg-white/60"
          onMouseDown={handleMouseDown}
          role="separator"
          aria-label="패널 크기 조절"
          aria-orientation="vertical"
          aria-valuemin={30}
          aria-valuemax={95}
          aria-valuenow={Math.round(width)}
        />

        {children}
      </div>
    </>
  );
}
