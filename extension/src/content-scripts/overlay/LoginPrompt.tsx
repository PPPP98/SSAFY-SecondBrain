import { GoogleLoginButton } from '@/content-scripts/overlay/GoogleLoginButton';

/**
 * 로그인 전 표시되는 프롬프트 컴포넌트
 * - Google 로그인 안내
 * - 다크 테마 디자인 (Shadow DOM 호환)
 */

export function LoginPrompt() {
  // Shadow DOM 호환 다크 테마 스타일
  const containerStyle: React.CSSProperties = {
    backgroundColor: '#2a2a2a',
    border: '1px solid #404040',
    borderRadius: '12px',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.5)',
    padding: '24px',
    width: '280px',
  };

  const titleStyle: React.CSSProperties = {
    color: '#ffffff',
    fontSize: '18px',
    fontWeight: '600',
    marginBottom: '8px',
    textAlign: 'center',
  };

  const subtitleStyle: React.CSSProperties = {
    color: '#b3b3b3',
    fontSize: '14px',
    marginBottom: '24px',
    textAlign: 'center',
  };

  const descriptionStyle: React.CSSProperties = {
    color: '#cccccc',
    fontSize: '14px',
    lineHeight: '1.5',
    marginBottom: '24px',
    textAlign: 'center',
  };

  const buttonWrapperStyle: React.CSSProperties = {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    overflow: 'visible', // SVG가 잘리지 않도록
  };

  return (
    <div style={containerStyle}>
      {/* 로고 또는 제목 */}
      <h3 style={titleStyle}>SecondBrain Extension</h3>
      <p style={subtitleStyle}>로그인이 필요합니다</p>

      {/* 안내 메시지 */}
      <p style={descriptionStyle}>
        웹페이지를 저장하고 노트를 생성하려면
        <br />
        먼저 로그인해주세요.
      </p>

      {/* Google 로그인 버튼 - Shadow DOM 환경에서 작동하도록 설정 */}
      <div style={buttonWrapperStyle}>
        <GoogleLoginButton text="signin" />
      </div>
    </div>
  );
}
