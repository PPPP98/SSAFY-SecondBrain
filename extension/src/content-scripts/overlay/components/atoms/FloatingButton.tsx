import { SimpleTooltip } from '@/content-scripts/overlay/components/ui/simple-tooltip';

/**
 * Floating Button (Atom)
 * - Overlay가 collapsed 상태일 때 표시
 * - 브라우저 우측 상단에 동그라미 버튼
 * - SecondBrain 아이콘 표시
 * - 라이트 모드 색상 고정, 테두리만 테마별로 다름
 */
interface FloatingButtonProps {
  onClick: () => void;
}

export function FloatingButton({ onClick }: FloatingButtonProps) {
  return (
    <div className="fixed top-4 right-4 z-[9999]">
      <SimpleTooltip content="SecondBrain 열기" side="bottom">
        <button
          onClick={onClick}
          className="inline-flex size-10 items-center justify-center rounded-full border-2 border-foreground/20 shadow-lg transition-transform hover:scale-110 dark:border-white"
          style={{ backgroundColor: 'oklch(0.25 0 0)' }}
        >
          <img
            src={chrome.runtime.getURL('/assets/icon.png')}
            alt="SecondBrain"
            className="h-6 w-6"
          />
        </button>
      </SimpleTooltip>
    </div>
  );
}
