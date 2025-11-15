/**
 * Spinner (Atom)
 * - 커스터마이징 가능한 로딩 스피너 컴포넌트
 * - 크기(size), 회전 속도(duration) 조정 가능
 * - Second Brain 로고 이미지 사용
 */

export interface SpinnerProps {
  /**
   * 스피너 크기
   * - 'sm': 64px (빠른 로딩 표시용)
   * - 'md': 96px (중간 크기, 기본값)
   * - 'lg': 120px (로그인 화면용)
   * - number: 직접 픽셀 지정
   */
  size?: 'sm' | 'md' | 'lg' | number;

  /**
   * 회전 애니메이션 지속 시간 (초 단위)
   * @default 3
   * @example 1 (빠른 회전), 6 (느린 회전)
   */
  duration?: number;

  /**
   * 추가 Tailwind CSS 클래스
   */
  className?: string;
}

const SIZE_CONFIG = {
  sm: {
    paddingClass: 'p-4',
    imageClass: 'h-16 w-16',
  },
  md: {
    paddingClass: 'p-5',
    imageClass: 'h-24 w-24',
  },
  lg: {
    paddingClass: 'p-6',
    imageClass: 'h-[120px] w-[120px]',
  },
} as const;

export function Spinner({ size = 'md', duration = 3, className = '' }: SpinnerProps) {
  // Custom size인 경우 inline style 사용
  if (typeof size === 'number') {
    const padding = Math.floor(size * 0.25);
    const imageSize = Math.floor(size * 0.5);

    return (
      <div
        className={`flex animate-spin items-center justify-center rounded-full bg-black ${className}`}
        style={{
          padding: `${padding}px`,
          animationDuration: `${duration}s`,
        }}
      >
        <img
          src={chrome.runtime.getURL('Logo_upscale.png')}
          alt="Loading"
          style={{
            width: `${imageSize}px`,
            height: `${imageSize}px`,
          }}
        />
      </div>
    );
  }

  // 사전 정의된 크기 사용
  const config = SIZE_CONFIG[size];

  return (
    <div
      className={`flex animate-spin items-center justify-center rounded-full bg-black ${config.paddingClass} ${className}`}
      style={{ animationDuration: `${duration}s` }}
    >
      <img
        src={chrome.runtime.getURL('Logo_upscale.png')}
        alt="Loading"
        className={`object-contain ${config.imageClass}`}
      />
    </div>
  );
}
