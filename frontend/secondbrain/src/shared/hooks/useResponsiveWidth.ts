import { useEffect, useState } from 'react';

interface UseResponsiveWidthOptions {
  /**
   * 브레이크포인트별 너비 설정
   * - key: 브레이크포인트 (px)
   * - value: 너비 (%)
   *
   * @example
   * ```
   * {
   *   1536: 40,  // 2xl
   *   1280: 50,  // xl
   *   1024: 66.67, // lg
   *   768: 75,   // md
   *   0: 100,    // mobile
   * }
   * ```
   */
  breakpoints: Record<number, number>;

  /**
   * 초기 너비 (기본: 50)
   */
  defaultWidth?: number;
}

interface UseResponsiveWidthReturn {
  /**
   * 현재 너비 (%)
   */
  width: number;

  /**
   * 수동으로 너비 설정하는 함수
   * - 드래그 리사이즈 등에서 사용
   */
  setWidth: React.Dispatch<React.SetStateAction<number>>;
}

/**
 * 브레이크포인트 기반 반응형 너비 관리 훅
 *
 * @description
 * - 화면 크기에 따라 자동으로 너비 조정
 * - TailwindCSS 브레이크포인트와 호환
 * - resize 이벤트 최적화 (debounce 불필요)
 * - 수동 너비 조정 지원 (드래그 리사이즈)
 *
 * @example
 * ```tsx
 * const { width, setWidth } = useResponsiveWidth({
 *   breakpoints: {
 *     1536: 40,  // 2xl: 화면이 1536px 이상일 때 40% 너비
 *     1280: 50,  // xl: 화면이 1280px 이상일 때 50% 너비
 *     1024: 66.67, // lg: 화면이 1024px 이상일 때 66.67% 너비
 *     768: 75,   // md: 화면이 768px 이상일 때 75% 너비
 *     0: 100,    // mobile: 화면이 768px 미만일 때 100% 너비
 *   },
 * });
 *
 * // 드래그 리사이즈
 * const handleDrag = (newWidth: number) => {
 *   setWidth(newWidth);
 * };
 *
 * return <div style={{ width: `${width}%` }}>Content</div>;
 * ```
 */
export function useResponsiveWidth({
  breakpoints,
  defaultWidth = 50,
}: UseResponsiveWidthOptions): UseResponsiveWidthReturn {
  const [width, setWidth] = useState(defaultWidth);

  useEffect(() => {
    const updateWidth = () => {
      const windowWidth = window.innerWidth;

      // 브레이크포인트를 내림차순으로 정렬
      const sortedBreakpoints = Object.keys(breakpoints)
        .map(Number)
        .sort((a, b) => b - a);

      // 현재 창 크기에 맞는 브레이크포인트 찾기
      for (const breakpoint of sortedBreakpoints) {
        if (windowWidth >= breakpoint) {
          setWidth(breakpoints[breakpoint]);
          return;
        }
      }

      // 브레이크포인트를 찾지 못하면 기본값 사용
      setWidth(defaultWidth);
    };

    // 초기 실행
    updateWidth();

    // resize 이벤트 리스너 등록
    window.addEventListener('resize', updateWidth);

    // cleanup
    return () => window.removeEventListener('resize', updateWidth);
  }, [breakpoints, defaultWidth]);

  return { width, setWidth };
}
