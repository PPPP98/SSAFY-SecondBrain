/**
 * 3D Logo Spinner 타입 정의
 * React Three Fiber 기반 로딩 스피너 컴포넌트
 */

/**
 * LogoSpinner 메인 컴포넌트 Props
 */
export interface LogoSpinnerProps {
  /**
   * 스피너 크기 (픽셀 단위)
   * @default 200
   */
  size?: number;

  /**
   * 회전 속도 배율
   * @default 0.8
   */
  rotationSpeed?: number;

  /**
   * Tailwind CSS 클래스명
   * @example "mx-auto mt-4"
   */
  className?: string;

  /**
   * 재질의 금속성 (0-1)
   * 높을수록 금속처럼 보임
   * @default 0.3
   */
  metalness?: number;

  /**
   * 재질의 거칠기 (0-1)
   * 낮을수록 광택이 남
   * @default 0.4
   */
  roughness?: number;

  /**
   * 텍스처 로딩 완료 시 호출되는 콜백
   */
  onLoad?: () => void;

  /**
   * 에러 발생 시 호출되는 콜백
   */
  onError?: (error: Error) => void;
}

/**
 * RotatingLogo 내부 컴포넌트 Props
 */
export interface RotatingLogoProps {
  /**
   * 회전 속도 배율
   */
  rotationSpeed: number;

  /**
   * 재질의 금속성 (0-1)
   */
  metalness: number;

  /**
   * 재질의 거칠기 (0-1)
   */
  roughness: number;

  /**
   * 텍스처 로딩 완료 콜백
   */
  onLoad?: () => void;
}

/**
 * Suspense Fallback 컴포넌트 Props
 */
export interface SpinnerFallbackProps {
  /**
   * 폴백 스피너 크기 (픽셀)
   */
  size?: number;

  /**
   * 폴백 메시지
   */
  message?: string;
}
