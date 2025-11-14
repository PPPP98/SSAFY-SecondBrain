import ReactDOM from 'react-dom/client';
import browser from 'webextension-polyfill';
import { DragSearchListener } from './DragSearchListener';
import { FloatingSearchButton } from './FloatingSearchButton';
import type {
  DragSearchMessage,
  DragSearchResponse,
  DragSearchSettings,
  FloatingButtonPosition,
} from '@/types/dragSearch';

// 기본 설정
const DEFAULT_SETTINGS: DragSearchSettings = {
  enabled: true,
  minTextLength: 2,
  debounceMs: 300,
  autoHideMs: 3000,
  excludedDomains: [],
};

/**
 * 드래그 검색 매니저
 * Content Script에서 드래그 검색 기능 총괄 관리
 */
class DragSearchManager {
  private listener: DragSearchListener | null = null;
  private floatingButtonRoot: ReactDOM.Root | null = null;
  private floatingButtonContainer: HTMLDivElement | null = null;
  private currentKeyword = '';
  private settings: DragSearchSettings = DEFAULT_SETTINGS;

  /**
   * 초기화
   */
  async initialize(): Promise<void> {
    // 설정 로드
    const stored = await browser.storage.local.get(['dragSearchSettings']);
    if (stored.dragSearchSettings) {
      this.settings = { ...DEFAULT_SETTINGS, ...stored.dragSearchSettings };
    }

    // 리스너 초기화
    this.listener = new DragSearchListener(this.settings, this.handleDragSearch);

    // Background로부터 응답 수신
    browser.runtime.onMessage.addListener(this.handleBackgroundMessage);

    // 설정 변경 메시지 수신
    window.addEventListener('message', this.handleSettingsUpdate);

    console.log('[SecondBrain] 드래그 검색 기능 활성화됨', this.settings);
  }

  /**
   * 드래그 감지 콜백
   */
  private handleDragSearch = (keyword: string, position: FloatingButtonPosition): void => {
    this.currentKeyword = keyword;
    this.showFloatingButton(position);
  };

  /**
   * 플로팅 버튼 표시
   */
  private showFloatingButton(position: FloatingButtonPosition): void {
    // 기존 버튼 제거
    this.hideFloatingButton();

    // 컨테이너 생성
    this.floatingButtonContainer = document.createElement('div');
    this.floatingButtonContainer.id = 'secondbrain-drag-search-button';
    document.body.appendChild(this.floatingButtonContainer);

    // React 렌더링
    this.floatingButtonRoot = ReactDOM.createRoot(this.floatingButtonContainer);
    this.floatingButtonRoot.render(
      <FloatingSearchButton
        position={position}
        keyword={this.currentKeyword}
        onSearch={this.handleSearchClick}
        onClose={this.hideFloatingButton}
        autoHideMs={this.settings.autoHideMs}
      />,
    );
  }

  /**
   * 검색 버튼 클릭 핸들러
   */
  private handleSearchClick = (): void => {
    // Background로 검색 요청
    const message: DragSearchMessage = {
      type: 'SEARCH_DRAG_TEXT',
      keyword: this.currentKeyword,
      timestamp: Date.now(),
    };

    browser.runtime.sendMessage(message).catch((error) => {
      console.error('[SecondBrain] 드래그 검색 메시지 전송 실패:', error);
    });

    this.hideFloatingButton();
  };

  /**
   * 플로팅 버튼 숨기기
   */
  private hideFloatingButton = (): void => {
    if (this.floatingButtonRoot) {
      this.floatingButtonRoot.unmount();
      this.floatingButtonRoot = null;
    }
    if (this.floatingButtonContainer) {
      this.floatingButtonContainer.remove();
      this.floatingButtonContainer = null;
    }
  };

  /**
   * Background로부터 검색 결과 수신
   */
  private handleBackgroundMessage = (message: unknown): void => {
    const response = message as DragSearchResponse;

    if (response.type === 'DRAG_SEARCH_RESULT') {
      // Overlay UI 열기 + 검색 결과 표시
      this.openOverlayWithResults(
        response.keyword,
        response.results || [],
        response.totalCount || 0,
      );
    } else if (response.type === 'DRAG_SEARCH_ERROR') {
      // 에러 표시
      console.error('[SecondBrain] 드래그 검색 에러:', response.error);

      // 에러 메시지를 Overlay로 전달
      window.postMessage(
        {
          type: 'SECONDBRAIN_DRAG_SEARCH_ERROR',
          payload: {
            keyword: response.keyword,
            error: response.error,
          },
        },
        '*',
      );
    }
  };

  /**
   * Overlay UI를 열고 검색 결과 전달
   */
  private openOverlayWithResults(keyword: string, results: unknown[], totalCount: number): void {
    // Overlay UI에 검색 결과 전달
    window.postMessage(
      {
        type: 'SECONDBRAIN_DRAG_SEARCH_RESULT',
        payload: {
          keyword,
          results,
          totalCount,
        },
      },
      '*',
    );
  }

  /**
   * 설정 업데이트 메시지 수신
   */
  private handleSettingsUpdate = (event: MessageEvent<{ type: string; settings?: unknown }>): void => {
    if (event.data?.type === 'DRAG_SEARCH_SETTINGS_UPDATED' && event.data.settings) {
      const newSettings = event.data.settings as DragSearchSettings;
      this.settings = newSettings;
      this.listener?.updateSettings(newSettings);
      console.log('[SecondBrain] 드래그 검색 설정 업데이트됨', newSettings);
    }
  };

  /**
   * Cleanup
   */
  destroy(): void {
    this.listener?.destroy();
    this.hideFloatingButton();
    browser.runtime.onMessage.removeListener(this.handleBackgroundMessage);
    window.removeEventListener('message', this.handleSettingsUpdate);
  }
}

// 초기화 실행
const manager = new DragSearchManager();
manager.initialize().catch((error) => {
  console.error('[SecondBrain] 드래그 검색 초기화 실패:', error);
});

// Hot Module Replacement 지원 (개발 환경)
if (import.meta.hot) {
  import.meta.hot.accept(() => {
    manager.destroy();
  });
}
