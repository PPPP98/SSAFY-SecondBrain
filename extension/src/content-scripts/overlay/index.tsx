import { createRoot } from 'react-dom/client';
import browser from 'webextension-polyfill';
import { OverlayRoot } from '@/content-scripts/overlay/OverlayRoot';

/**
 * Content Script 엔트리포인트
 * - Shadow DOM 생성 및 스타일 격리
 * - React 앱 마운트
 * - Tailwind CSS + Glass styles 주입
 * - Background와 메시지 통신
 */

// Overlay 토글 상태 관리
let isOverlayVisible = false;

// Shadow DOM 컨테이너 생성
const container = document.createElement('div');
container.id = 'secondbrain-extension-container';

// Shadow DOM 생성 (스타일 격리)
const shadowRoot = container.attachShadow({ mode: 'open' });

// Shadow DOM에 주입할 전체 스타일 시스템
const style = document.createElement('style');
style.textContent = `
  /* Reset & Base Styles */
  * {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
  }

  /* Font System */
  body, button, input {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  }

  /* Dark Theme Variables */
  :root {
    --bg-primary: #1a1a1a;
    --bg-secondary: #2a2a2a;
    --bg-tertiary: #333333;
    --bg-hover: #404040;

    --border-primary: #404040;
    --border-accent: #4285f4;

    --text-primary: #ffffff;
    --text-secondary: #b3b3b3;
    --text-muted: #808080;

    --shadow-lg: 0 4px 12px rgba(0, 0, 0, 0.5);
  }

  /* Animation Keyframes */
  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(-10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  @keyframes slideDown {
    from {
      opacity: 0;
      transform: translateY(-20px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  /* Utility Classes for future use */
  .ext-animate-spin {
    animation: spin 1s linear infinite;
  }

  .ext-animate-fadeIn {
    animation: fadeIn 0.2s ease-out;
  }

  .ext-animate-slideDown {
    animation: slideDown 0.3s ease-out;
  }

  /* Extension-specific resets */
  #secondbrain-extension-root {
    all: initial;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  }

  #secondbrain-extension-root * {
    all: unset;
    display: revert;
    box-sizing: border-box;
  }

  #secondbrain-extension-root button {
    cursor: pointer;
  }
`;

shadowRoot.appendChild(style);

// React 앱 마운트 포인트
const appRoot = document.createElement('div');
shadowRoot.appendChild(appRoot);

// 컨테이너를 body에 추가
document.body.appendChild(container);

// React 앱 렌더링
const root = createRoot(appRoot);

// 초기 상태로 렌더링
function renderOverlay(visible: boolean): void {
  isOverlayVisible = visible;
  root.render(<OverlayRoot isOpen={visible} onToggle={setOverlayVisible} />);
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

console.log('SecondBrain Extension Overlay loaded');
