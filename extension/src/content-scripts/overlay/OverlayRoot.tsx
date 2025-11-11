import { ExtensionOverlay } from '@/content-scripts/overlay/ExtensionOverlay';

/**
 * Shadow DOM 내부에 렌더링되는 React 루트 컴포넌트
 * - ExtensionOverlay를 감싸는 최상위 컴포넌트
 * - 향후 전역 상태 Provider 추가 가능
 */

interface OverlayRootProps {
  isOpen: boolean;
  onToggle: (visible: boolean) => void;
}

export function OverlayRoot({ isOpen, onToggle }: OverlayRootProps) {
  return <ExtensionOverlay isOpen={isOpen} onToggle={onToggle} />;
}
