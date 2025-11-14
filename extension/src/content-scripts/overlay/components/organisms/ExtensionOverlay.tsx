import { useEffect, useState } from 'react';
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
 * - Smooth expand/collapse animations
 */
interface ExtensionOverlayProps {
  isOpen: boolean;
  onToggle: (visible: boolean) => void;
}

export function ExtensionOverlay({ isOpen, onToggle }: ExtensionOverlayProps) {
  const { loading, authenticated, user, logout } = useExtensionAuth();
  const { isExpanded, isCollapsed, isHidden, expand, collapse } = useOverlayState();
  const [isAnimating, setIsAnimating] = useState(false);
  const [animationPhase, setAnimationPhase] = useState<'idle' | 'expanding' | 'collapsing'>('idle');
  const [isLoggingOut, setIsLoggingOut] = useState(false);

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

  // Animation handling for expand/collapse
  const handleExpand = () => {
    setAnimationPhase('expanding');
    setIsAnimating(true);
    expand();
    setTimeout(() => {
      setIsAnimating(false);
      setAnimationPhase('idle');
    }, 300);
  };

  const handleCollapse = () => {
    setAnimationPhase('collapsing');
    setIsAnimating(true);
    setTimeout(() => {
      collapse();
      setIsAnimating(false);
      setAnimationPhase('idle');
    }, 300);
  };

  const handleLogout = async () => {
    setIsLoggingOut(true);
    try {
      await logout();
    } finally {
      // Keep showing animation for a bit longer for visual feedback
      setTimeout(() => {
        setIsLoggingOut(false);
      }, 500);
    }
  };

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
          onLogout={() => void handleLogout()}
        />

        <div className="p-4">
          {(loading || isLoggingOut) && (
            <div className="flex flex-col items-center justify-center p-8">
              <img
                src={chrome.runtime.getURL('Logo_upscale.png')}
                alt="Loading"
                className="h-16 w-16 animate-spin object-contain"
                style={{ animationDuration: '1s' }}
              />
              <p className="mt-4 text-sm text-muted-foreground">
                {isLoggingOut ? '로그아웃 중...' : '로딩 중...'}
              </p>
            </div>
          )}

          {!loading && !isLoggingOut && !authenticated && <LoginPrompt />}

          {!loading && !isLoggingOut && authenticated && <ActionButtons />}
        </div>
      </div>
    </div>
  );
}
