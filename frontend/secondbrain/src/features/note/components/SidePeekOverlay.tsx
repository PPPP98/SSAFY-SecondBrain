import { type ReactNode, useState } from 'react';

interface SidePeekOverlayProps {
  isOpen: boolean;
  onClose: () => void;
  children: ReactNode;
  mode: 'full-screen' | 'side-peek';
  onToggleMode: () => void;
}

/**
 * Side Peek 공통 오버레이 컨테이너
 * - 배경 오버레이 (클릭 시 닫기)
 * - 슬라이드 애니메이션 (왼쪽→오른쪽)
 * - TailwindCSS transform 및 반응형 클래스 사용
 * - 크기 조절 가능 (왼쪽 border 드래그, side-peek 모드에서만)
 * - 전체화면/부분화면 모드 지원
 */
export function SidePeekOverlay({ isOpen, onClose, children, mode }: SidePeekOverlayProps) {
  // 드래그로 설정한 커스텀 너비 (null이면 CSS 반응형 사용)
  const [customWidth, setCustomWidth] = useState<number | null>(null);

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
            ? 'pointer-events-auto translate-x-0 animate-slide-in-right opacity-100'
            : 'pointer-events-none translate-x-full opacity-0'
        } ${mode === 'full-screen' ? 'w-full' : 'w-full md:w-3/4 lg:w-2/3 xl:w-1/2 2xl:w-2/5'}`}
        style={mode === 'side-peek' && customWidth ? { width: `${customWidth}%` } : undefined}
        role="dialog"
        aria-modal="true"
      >
        {/* Resize Handle - side-peek 모드에서만 표시 */}
        {mode === 'side-peek' && (
          <div
            className="absolute left-0 top-0 z-[120] h-full w-2 cursor-ew-resize transition-colors hover:bg-white/40 active:bg-white/60"
            onMouseDown={(e) => {
              e.preventDefault();

              // 클로저 변수로 드래그 상태 추적 (ref 불필요)
              const startX = e.clientX;
              const panelElement = e.currentTarget.parentElement!;
              const startWidthPx = panelElement.offsetWidth;
              const windowWidth = window.innerWidth;

              // 드래그 중 크기 조절 (인라인 함수, useCallback 불필요)
              const handleMove = (moveEvent: MouseEvent) => {
                const deltaX = startX - moveEvent.clientX; // 왼쪽으로 드래그하면 +
                const newWidthPx = startWidthPx + deltaX;
                const newWidthPercent = (newWidthPx / windowWidth) * 100;

                // 최소 30%, 최대 95%
                const clampedWidth = Math.min(Math.max(newWidthPercent, 30), 95);
                setCustomWidth(clampedWidth);
              };

              // 드래그 종료 (클린업 포함)
              const handleUp = () => {
                document.removeEventListener('mousemove', handleMove);
                document.removeEventListener('mouseup', handleUp);
                document.body.style.userSelect = '';
              };

              // 이벤트 리스너 등록 (useEffect 불필요)
              document.body.style.userSelect = 'none';
              document.addEventListener('mousemove', handleMove);
              document.addEventListener('mouseup', handleUp);
            }}
            onDoubleClick={() => {
              // 더블클릭으로 커스텀 너비 리셋 (CSS 반응형으로 복귀)
              setCustomWidth(null);
            }}
            role="separator"
            aria-label="패널 크기 조절 (더블클릭으로 초기화)"
            aria-orientation="vertical"
            aria-valuemin={30}
            aria-valuemax={95}
            aria-valuenow={customWidth ? Math.round(customWidth) : undefined}
          />
        )}

        {children}
      </div>
    </>
  );
}
