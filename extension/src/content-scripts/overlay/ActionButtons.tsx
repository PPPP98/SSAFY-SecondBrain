import { useState } from 'react';

/**
 * 로그인 후 표시되는 액션 버튼 컴포넌트
 * - 페이지 추가하기 버튼
 * - 저장하기 버튼
 * - 다크 테마 디자인 (Shadow DOM 호환)
 */

interface ActionButtonsProps {
  onClose: () => void;
}

export function ActionButtons({ onClose }: ActionButtonsProps) {
  const [hoveredButton, setHoveredButton] = useState<string | null>(null);

  function handleAddPage(): void {
    // TODO: 현재 페이지를 SecondBrain에 추가하는 로직
    console.log('Add page:', window.location.href);
  }

  function handleSave(): void {
    // TODO: 현재 페이지를 저장하는 로직
    console.log('Save page:', window.location.href);
  }

  // Shadow DOM 호환 다크 테마 스타일
  const containerStyle: React.CSSProperties = {
    position: 'relative',
    backgroundColor: '#2a2a2a',
    border: '1px solid #404040',
    borderRadius: '12px',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.5)',
    padding: '16px',
    paddingTop: '40px',
    width: '260px',
  };

  const closeButtonStyle: React.CSSProperties = {
    position: 'absolute',
    top: '8px',
    right: '8px',
    width: '24px',
    height: '24px',
    backgroundColor: 'transparent',
    border: 'none',
    cursor: 'pointer',
    color: '#808080',
    fontSize: '20px',
    lineHeight: '1',
    transition: 'color 0.2s',
  };

  const buttonStyle = (isHovered: boolean): React.CSSProperties => ({
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '8px',
    width: '100%',
    padding: '10px 16px',
    backgroundColor: isHovered ? '#404040' : '#333333',
    border: isHovered ? '1px solid #4285f4' : '1px solid #404040',
    borderRadius: '8px',
    color: '#ffffff',
    fontSize: '14px',
    fontWeight: '500',
    cursor: 'pointer',
    transition: 'all 0.2s ease',
    marginBottom: '8px',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  });

  const iconStyle: React.CSSProperties = {
    width: '18px',
    height: '18px',
    fill: 'currentColor',
  };

  return (
    <div style={containerStyle}>
      {/* 닫기 버튼 */}
      <button type="button" onClick={onClose} style={closeButtonStyle} aria-label="닫기">
        ×
      </button>

      {/* 페이지 추가 버튼 */}
      <button
        type="button"
        onClick={handleAddPage}
        style={buttonStyle(hoveredButton === 'add')}
        onMouseEnter={() => setHoveredButton('add')}
        onMouseLeave={() => setHoveredButton(null)}
      >
        <svg style={iconStyle} viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 11h-4v4h-2v-4H7v-2h4V7h2v4h4v2z" />
        </svg>
        <span>페이지 추가하기</span>
      </button>

      {/* 저장하기 버튼 */}
      <button
        type="button"
        onClick={handleSave}
        style={{
          ...buttonStyle(hoveredButton === 'save'),
          marginBottom: 0,
        }}
        onMouseEnter={() => setHoveredButton('save')}
        onMouseLeave={() => setHoveredButton(null)}
      >
        <svg style={iconStyle} viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
          />
        </svg>
        <span>저장하기</span>
      </button>
    </div>
  );
}
