import { useEffect, useState } from 'react';
import { Toolbar } from '@/content-scripts/overlay/components/molecules/Toolbar';
import { AuthCard } from '@/content-scripts/overlay/components/molecules/AuthCard';
import { ActionButtons } from '@/content-scripts/overlay/components/molecules/ActionButtons';
import { FloatingButton } from '@/content-scripts/overlay/components/atoms/FloatingButton';
import { DragSearchPanel } from '@/content-scripts/overlay/components/organisms/DragSearchPanel';
import { URLListModal } from '@/content-scripts/overlay/components/organisms/URLListModal';
import { SaveStatusPanel } from '@/content-scripts/overlay/components/organisms/SaveStatusPanel';
import { DragSearchSettingsPanel } from '@/content-scripts/overlay/components/organisms/DragSearchSettingsPanel';
import { useExtensionAuth } from '@/hooks/useExtensionAuth';
import { useOverlayState } from '@/hooks/useOverlayState';
import { usePageCollectionStore } from '@/stores/pageCollectionStore';
import { useDragSearchStore } from '@/stores/dragSearchStore';
import * as storage from '@/services/storageService';

type ActivePanel = null | 'urlList' | 'saveStatus' | 'settings';

/**
 * Extension Overlay (Organism)
 * - Chrome 확장프로그램 메인 Overlay 컴포넌트
 * - 인증 상태에 따라 LoginPrompt 또는 ActionButtons 렌더링
 * - Collapsed/Expanded 상태 관리
 * - Outside click으로 닫기 기능
 * - Smooth expand/collapse animations
 */
interface ExtensionOverlayProps {
  isOpen: boolean;
  onToggle: (visible: boolean) => void;
}

export function ExtensionOverlay({ isOpen, onToggle }: ExtensionOverlayProps) {
  const { loading, authenticated, user, logout } = useExtensionAuth();
  const { isExpanded, isCollapsed, isHidden, expand, collapse } = useOverlayState();
  const { initialize, syncFromStorage, getPageList, removePage, clearPages } =
    usePageCollectionStore();
  const {
    keyword,
    results,
    totalCount,
    isLoading,
    error,
    isVisible: isDragSearchVisible,
  } = useDragSearchStore();
  const [isAnimating, setIsAnimating] = useState(false);
  const [animationPhase, setAnimationPhase] = useState<'idle' | 'expanding' | 'collapsing'>('idle');
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [activePanel, setActivePanel] = useState<ActivePanel>(null);

  // Storage 초기화 (마운트 시 한 번만)
  useEffect(() => {
    void (async () => {
      // 1. Storage 마이그레이션 및 초기화
      await storage.initializeStorage();

      // 2. Store 초기화
      await initialize();
    })();
  }, [initialize]);

  // Storage 변경 감지 → 탭 간 동기화
  useEffect(() => {
    const unwatch = storage.watchStorageChanges((key, newValue) => {
      if (key === storage.STORAGE_KEYS.COLLECTED_PAGES) {
        // 다른 탭에서 페이지 목록이 변경됨 → Store 동기화
        syncFromStorage(newValue as string[]);
      }
    });

    // Cleanup
    return unwatch;
  }, [syncFromStorage]);

  // Escape 키로 닫기
  useEffect(() => {
    if (!isExpanded || !isOpen) return;

    function handleEscape(event: KeyboardEvent): void {
      if (event.key === 'Escape') {
        onToggle(false);
      }
    }

    document.addEventListener('keydown', handleEscape);

    return () => {
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isExpanded, isOpen, onToggle]);

  // Animation handling for expand/collapse
  const handleExpand = () => {
    setAnimationPhase('expanding');
    setIsAnimating(true);
    void expand();
    onToggle(true); // 부모의 isOpen 상태도 함께 업데이트
    setTimeout(() => {
      setIsAnimating(false);
      setAnimationPhase('idle');
    }, 300);
  };

  const handleCollapse = () => {
    setAnimationPhase('collapsing');
    setIsAnimating(true);
    setTimeout(() => {
      void collapse();
      setIsAnimating(false);
      setAnimationPhase('idle');
    }, 300);
  };

  const handleLogout = () => {
    setIsLoggingOut(true);
    void (async () => {
      try {
        await logout();
      } finally {
        // Keep showing animation for a bit longer for visual feedback
        setTimeout(() => {
          setIsLoggingOut(false);
        }, 500);
      }
    })();
  };

  function handleTogglePanel(panel: 'urlList' | 'saveStatus' | 'settings'): void {
    setActivePanel((prev) => (prev === panel ? null : panel));
  }

  // Collapsed 상태: Floating 버튼만 표시 (with animation)
  if (isCollapsed && !isAnimating) {
    return (
      <div
        style={{
          position: 'fixed',
          top: '16px',
          right: '16px',
          zIndex: 9999,
          animation:
            animationPhase === 'idle' ? 'slideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1)' : undefined,
        }}
      >
        <FloatingButton onClick={handleExpand} />
      </div>
    );
  }

  // Hidden 상태 또는 isOpen이 false: 아무것도 표시 안함
  if (isHidden || (!isOpen && !isAnimating)) {
    return null;
  }

  // Expanded 상태 또는 애니메이션 중: 전체 UI 표시
  return (
    <div
      className="fixed top-4 right-4 z-[9999]"
      style={{
        opacity: animationPhase === 'collapsing' ? 0 : 1,
        transform:
          animationPhase === 'expanding'
            ? 'scale(1) translateY(0)'
            : animationPhase === 'collapsing'
              ? 'scale(0.95) translateY(-10px)'
              : 'scale(1) translateY(0)',
        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        pointerEvents: isOpen && isExpanded && !isAnimating ? 'auto' : 'none',
      }}
    >
      <div className="extension-overlay-border rounded-xl bg-background shadow-2xl">
        <Toolbar
          authenticated={authenticated}
          user={user}
          onCollapse={handleCollapse}
          onClose={() => {
            onToggle(false);
          }}
          onLogout={handleLogout}
        />

        <div className="p-4">
          {/* 드래그 검색 결과 패널 (인증 완료 후 표시) */}
          {authenticated && !loading && !isLoggingOut && isDragSearchVisible && (
            <div className="mb-4">
              <DragSearchPanel
                keyword={keyword}
                results={results}
                totalCount={totalCount}
                isLoading={isLoading}
                error={error}
              />
            </div>
          )}

          {/* Transition wrapper - 부드러운 크기 조절 애니메이션 */}
          <div className="relative">
            {/* AuthCard wrapper - overflow-hidden 개별 적용 */}
            <div className="overflow-hidden">
              <div
                className={`transition-all duration-500 ease-in-out ${
                  loading || isLoggingOut || !authenticated
                    ? 'translate-y-0 opacity-100'
                    : 'pointer-events-none absolute top-0 left-0 w-full -translate-y-full opacity-0'
                }`}
              >
                <AuthCard
                  state={loading || isLoggingOut ? 'loading' : 'login'}
                  message={isLoggingOut ? '로그아웃 중...' : '로딩 중...'}
                />
              </div>
            </div>

            {/* ActionButtons - 아래에서 슬라이드 + 페이드 인 */}
            <div
              className={`transition-all duration-500 ease-in-out ${
                !loading && !isLoggingOut && authenticated
                  ? 'translate-y-0 opacity-100'
                  : 'pointer-events-none translate-y-full opacity-0'
              }`}
            >
              {!loading && !isLoggingOut && authenticated && (
                <ActionButtons activePanel={activePanel} onTogglePanel={handleTogglePanel} />
              )}
            </div>

            {/* 패널 영역 - 드롭다운처럼 펼쳐짐 */}
            {!loading && !isLoggingOut && authenticated && (
              <div
                className={`overflow-hidden transition-all duration-300 ease-in-out ${
                  activePanel === 'settings'
                    ? 'mt-4 max-h-[600px] opacity-100'
                    : activePanel
                      ? 'mt-4 max-h-[400px] opacity-100'
                      : 'max-h-0 opacity-0'
                }`}
              >
                {activePanel && (
                  <div className="relative">
                    {/* URLListModal */}
                    <div
                      className={`transition-opacity duration-150 ${
                        activePanel === 'urlList'
                          ? 'opacity-100'
                          : 'pointer-events-none absolute inset-0 opacity-0'
                      }`}
                    >
                      <URLListModal
                        isOpen={activePanel === 'urlList'}
                        onClose={() => setActivePanel(null)}
                        urls={getPageList()}
                        onRemove={(url: string) => {
                          void removePage(url);
                        }}
                        onClearAll={() => {
                          void clearPages();
                          setActivePanel(null);
                        }}
                      />
                    </div>

                    {/* SaveStatusPanel */}
                    <div
                      className={`transition-opacity duration-150 ${
                        activePanel === 'saveStatus'
                          ? 'opacity-100'
                          : 'pointer-events-none absolute inset-0 opacity-0'
                      }`}
                    >
                      <SaveStatusPanel
                        isOpen={activePanel === 'saveStatus'}
                        onClose={() => setActivePanel(null)}
                      />
                    </div>

                    {/* DragSearchSettingsPanel */}
                    <div
                      className={`transition-opacity duration-150 ${
                        activePanel === 'settings'
                          ? 'opacity-100'
                          : 'pointer-events-none absolute inset-0 opacity-0'
                      }`}
                    >
                      <DragSearchSettingsPanel onClose={() => setActivePanel(null)} />
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
