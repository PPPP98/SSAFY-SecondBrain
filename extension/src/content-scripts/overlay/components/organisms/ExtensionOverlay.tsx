import { useEffect } from 'react';
import { Toolbar } from '@/content-scripts/overlay/components/molecules/Toolbar';
import { LoginPrompt } from '@/content-scripts/overlay/components/molecules/LoginPrompt';
import { ActionButtons } from '@/content-scripts/overlay/components/molecules/ActionButtons';
import { FloatingButton } from '@/content-scripts/overlay/components/atoms/FloatingButton';
import { useExtensionAuth } from '@/hooks/useExtensionAuth';
import { useOverlayState } from '@/hooks/useOverlayState';

/**
 * Extension Overlay (Organism)
 * - Chrome 확장프로그램 메인 Overlay 컴포넌트
 * - 인증 상태에 따라 LoginPrompt 또는 ActionButtons 렌더링
 * - Collapsed/Expanded 상태 관리
 * - Outside click으로 닫기 기능
 */
interface ExtensionOverlayProps {
  isOpen: boolean;
  onToggle: (visible: boolean) => void;
}

export function ExtensionOverlay({ isOpen, onToggle }: ExtensionOverlayProps) {
  const { loading, authenticated, user, logout } = useExtensionAuth();
  const { isExpanded, isCollapsed, isHidden, expand, collapse } = useOverlayState();

  // Outside click 처리 - Shadow DOM에서는 비활성화
  // (Shadow DOM 내부/외부 클릭 감지가 복잡하므로 Escape 키로만 닫기)
  useEffect(() => {
    // Outside click은 비활성화
    return;
  }, [isExpanded, isOpen, onToggle]);

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

  // Collapsed 상태: Floating 버튼만 표시
  if (isCollapsed) {
    return <FloatingButton onClick={expand} />;
  }

  // Hidden 상태 또는 isOpen이 false: 아무것도 표시 안함
  if (isHidden || !isOpen) {
    return null;
  }

  // Expanded 상태: 전체 UI 표시
  return (
    <div
      className="fixed top-4 right-4 z-[9999] transition-all duration-200 ease-out"
      style={{
        opacity: isOpen && isExpanded ? 1 : 0,
        transform: isOpen && isExpanded ? 'translateY(0)' : 'translateY(-10px)',
        pointerEvents: isOpen && isExpanded ? 'auto' : 'none',
      }}
    >
      <div className="extension-overlay-border rounded-xl bg-background shadow-2xl">
        <Toolbar
          authenticated={authenticated}
          user={user}
          onCollapse={collapse}
          onClose={() => {
            onToggle(false);
          }}
          onLogout={() => void logout()}
        />

        <div className="p-4">
          {loading && (
            <div className="flex items-center justify-center p-8">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
          )}

          {!loading && !authenticated && <LoginPrompt />}

          {!loading && authenticated && <ActionButtons />}
        </div>
      </div>
    </div>
  );
}
