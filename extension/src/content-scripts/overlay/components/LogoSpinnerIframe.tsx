import { useEffect, useRef, useState } from 'react';

interface LogoSpinnerIframeProps {
  /**
   * 스피너 크기 (픽셀)
   * @default 120
   */
  size?: number;

  /**
   * Tailwind 클래스
   */
  className?: string;

  /**
   * 로딩 완료 콜백
   */
  onLoad?: () => void;
}

/**
 * iframe 기반 Three.js 3D 로고 스피너
 *
 * Content Script의 Isolated World 제약을 우회하기 위해
 * Extension 페이지를 iframe으로 로드하여 WebGL 사용
 *
 * @example
 * ```tsx
 * <LogoSpinnerIframe size={120} />
 * ```
 */
export function LogoSpinnerIframe({ size = 120, className = '', onLoad }: LogoSpinnerIframeProps) {
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    const handleLoad = () => {
      setIsLoaded(true);
      onLoad?.();
    };

    const iframe = iframeRef.current;
    if (iframe) {
      iframe.addEventListener('load', handleLoad);
      return () => iframe.removeEventListener('load', handleLoad);
    }
  }, [onLoad]);

  return (
    <div
      className={className}
      style={{
        width: size,
        height: size,
        position: 'relative',
      }}
    >
      {/* Three.js iframe */}
      <iframe
        ref={iframeRef}
        src={chrome.runtime.getURL('threejs-spinner.html')}
        style={{
          width: '100%',
          height: '100%',
          border: 'none',
          borderRadius: '50%',
          background: 'transparent',
          opacity: isLoaded ? 1 : 0,
          transition: 'opacity 0.3s ease-in-out',
        }}
        title="3D Logo Spinner"
      />

      {/* 로딩 폴백 */}
      {!isLoaded && (
        <div
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <div
            className="animate-spin rounded-full border-4 border-gray-300 border-t-indigo-600"
            style={{
              width: size * 0.3,
              height: size * 0.3,
            }}
          />
        </div>
      )}
    </div>
  );
}
