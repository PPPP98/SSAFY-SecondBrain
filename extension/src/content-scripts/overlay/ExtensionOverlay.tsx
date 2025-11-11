import { useEffect } from 'react';
import { useExtensionAuth } from '@/hooks/useExtensionAuth';
import { LoginPrompt } from '@/content-scripts/overlay/LoginPrompt';
import { ActionButtons } from '@/content-scripts/overlay/ActionButtons';

/**
 * Chrome 확장프로그램 메인 Overlay 컴포넌트
 * - 확장프로그램 아이콘 클릭 시 표시
 * - 인증 상태에 따라 LoginPrompt 또는 ActionButtons 렌더링
 * - Outside click으로 닫기 기능
 */

interface ExtensionOverlayProps {
  isOpen: boolean;
  onToggle: (visible: boolean) => void;
}

export function ExtensionOverlay({ isOpen, onToggle }: ExtensionOverlayProps) {
  const { loading, authenticated } = useExtensionAuth();

  // Outside click 처리
  useEffect(() => {
    if (!isOpen) return;

    function handleClickOutside(event: MouseEvent): void {
      const target = event.target as HTMLElement;
      const overlayRoot = document.getElementById('secondbrain-extension-root');

      if (overlayRoot && !overlayRoot.contains(target)) {
        onToggle(false);
      }
    }

    // 약간의 딜레이 후 이벤트 리스너 추가 (현재 클릭 이벤트 방지)
    const timeoutId = setTimeout(() => {
      document.addEventListener('click', handleClickOutside);
    }, 100);

    return () => {
      clearTimeout(timeoutId);
      document.removeEventListener('click', handleClickOutside);
    };
  }, [isOpen, onToggle]);

  // Escape 키로 닫기
  useEffect(() => {
    if (!isOpen) return;

    function handleEscape(event: KeyboardEvent): void {
      if (event.key === 'Escape') {
        onToggle(false);
      }
    }

    document.addEventListener('keydown', handleEscape);

    return () => {
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen, onToggle]);

  function handleClose(): void {
    onToggle(false);
  }

  if (!isOpen) return null;

  // Shadow DOM 호환 inline styles
  const rootStyle: React.CSSProperties = {
    position: 'fixed',
    right: '16px',
    top: '16px',
    zIndex: 9999,
    transition: 'all 0.2s ease-out',
    opacity: isOpen ? 1 : 0,
    transform: isOpen ? 'translateY(0)' : 'translateY(-10px)',
    pointerEvents: isOpen ? 'auto' : 'none',
  };

  const loadingContainerStyle: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '32px',
    backgroundColor: '#2a2a2a',
    borderRadius: '12px',
    border: '1px solid #404040',
  };

  const spinnerStyle: React.CSSProperties = {
    width: '32px',
    height: '32px',
    border: '4px solid rgba(255, 255, 255, 0.2)',
    borderTopColor: '#ffffff',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  };

  return (
    <>
      <style>{`
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}</style>
      <div id="secondbrain-extension-root" style={rootStyle}>
        {loading && (
          <div style={loadingContainerStyle}>
            <div style={spinnerStyle} />
          </div>
        )}

        {!loading && !authenticated && <LoginPrompt />}

        {!loading && authenticated && <ActionButtons onClose={handleClose} />}
      </div>
    </>
  );
}
