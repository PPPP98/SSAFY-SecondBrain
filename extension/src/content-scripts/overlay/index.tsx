import { createRoot } from 'react-dom/client';
import browser from 'webextension-polyfill';
import { OverlayRoot } from '@/content-scripts/overlay/OverlayRoot';
import overlayStyles from '@/content-scripts/overlay.css?inline';

/**
 * Content Script 엔트리포인트
 * - Shadow DOM 생성 및 스타일 격리
 * - React 앱 마운트
 * - Tailwind CSS + Shadcn UI styles inline 주입
 * - Background와 메시지 통신
 */

export function onExecute() {
  // Overlay 토글 상태 관리
  let isOverlayVisible = false;

  try {
    // Shadow DOM 컨테이너 생성
    const container = document.createElement('div');
    container.id = 'secondbrain-extension-container';

    // Shadow DOM 생성 (스타일 격리)
    const shadowRoot = container.attachShadow({ mode: 'open' });

    // Tailwind CSS를 Shadow DOM에 주입 (inline import)
    const style = document.createElement('style');
    style.textContent = overlayStyles;
    shadowRoot.appendChild(style);

    // React 앱 마운트 포인트 (ThemeContext에서 찾을 수 있도록 id 설정)
    const appRoot = document.createElement('div');
    appRoot.id = 'secondbrain-extension-root';
    shadowRoot.appendChild(appRoot);

    // 컨테이너를 body에 추가
    document.body.appendChild(container);

    // React 앱 렌더링
    const root = createRoot(appRoot);

    // 초기 상태로 렌더링
    function renderOverlay(visible: boolean): void {
      isOverlayVisible = visible;
      root.render(
        <OverlayRoot isOpen={visible} onToggle={setOverlayVisible} shadowRoot={shadowRoot} />,
      );
    }

    function setOverlayVisible(visible: boolean): void {
      renderOverlay(visible);
    }

    // 초기 렌더링 (닫힌 상태)
    renderOverlay(false);

    // Background로부터 메시지 수신 (Content Script 레벨)
    browser.runtime.onMessage.addListener((message: unknown, _sender, sendResponse) => {
      const msg = message as { type: string };

      if (msg.type === 'TOGGLE_OVERLAY') {
        renderOverlay(!isOverlayVisible);
        sendResponse({ success: true });
      } else if (msg.type === 'PING') {
        // Content Script가 활성화되어 있음을 알리는 응답
        sendResponse({ pong: true });
      }

      return true; // 비동기 응답을 위해 true 반환
    });
  } catch (error) {
    console.error('❌ [Content Script] Fatal error:', error);
    console.error('Stack trace:', (error as Error).stack);
  }
}
