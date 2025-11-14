import { ExtensionOverlay } from '@/content-scripts/overlay/components/organisms/ExtensionOverlay';
import { ThemeProvider } from '@/contexts/ThemeContext';
import { ShadowRootProvider } from '@/contexts/ShadowRootContext';
import { SimpleToastContainer } from '@/content-scripts/overlay/components/molecules/SimpleToast';
import { useEffect } from 'react';

/**
 * Shadow DOM 내부에 렌더링되는 React 루트 컴포넌트
 * - ShadowRootProvider로 Shadow Root 컨텍스트 제공
 * - ThemeProvider로 전체 앱을 감싸서 Light/Dark 모드 지원
 * - ExtensionOverlay를 렌더링
 * - Animation keyframes 주입
 */

interface OverlayRootProps {
  isOpen: boolean;
  onToggle: (visible: boolean) => void;
  shadowRoot: ShadowRoot | HTMLElement;
}

export function OverlayRoot({ isOpen, onToggle, shadowRoot }: OverlayRootProps) {
  // Inject animation keyframes into Shadow DOM
  useEffect(() => {
    if (shadowRoot instanceof ShadowRoot) {
      const styleId = 'overlay-animations';
      if (!shadowRoot.querySelector(`#${styleId}`)) {
        const style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
          @keyframes slideIn {
            from {
              opacity: 0;
              transform: translateX(20px);
            }
            to {
              opacity: 1;
              transform: translateX(0);
            }
          }

          @keyframes fadeIn {
            from {
              opacity: 0;
              transform: scale(0.95);
            }
            to {
              opacity: 1;
              transform: scale(1);
            }
          }

          @keyframes pulse {
            0%, 100% {
              opacity: 1;
            }
            50% {
              opacity: 0.8;
            }
          }
        `;
        shadowRoot.appendChild(style);
      }
    }
  }, [shadowRoot]);

  return (
    <ShadowRootProvider shadowRoot={shadowRoot}>
      <ThemeProvider>
        <ExtensionOverlay isOpen={isOpen} onToggle={onToggle} />
        <SimpleToastContainer />
      </ThemeProvider>
    </ShadowRootProvider>
  );
}
