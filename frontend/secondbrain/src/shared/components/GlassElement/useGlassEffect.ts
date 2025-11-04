import { useRef, useEffect } from 'react';

/**
 * Glass 효과를 위한 마우스 이벤트 핸들링 커스텀 훅
 *
 * @description
 * - wrapper 요소에 hover 시 마우스 위치 기반 glass border 그라데이션 효과 적용
 * - hover 상태일 때만 이벤트 리스너 활성화 (성능 최적화)
 * - 각 컴포넌트 인스턴스가 독립적으로 동작
 *
 * @returns refs - wrapper, border1, border2 ref 객체
 */
export function useGlassEffect() {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const border1Ref = useRef<HTMLSpanElement>(null);
  const border2Ref = useRef<HTMLSpanElement>(null);
  const isHoveredRef = useRef(false);

  useEffect(() => {
    const wrapper = wrapperRef.current;
    if (!wrapper) return;

    const handleMouseEnter = () => {
      isHoveredRef.current = true;
    };

    const handleMouseLeave = () => {
      isHoveredRef.current = false;
    };

    const handleMouseMove = (e: MouseEvent) => {
      // hover 상태가 아니면 계산하지 않음 (성능 최적화)
      if (!isHoveredRef.current) return;
      if (!border1Ref.current || !border2Ref.current) return;

      const rect = wrapper.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;

      let mouseX = ((e.clientX - centerX) / rect.width) * 100;
      let mouseY = ((e.clientY - centerY) / rect.height) * 100;

      mouseX = Math.max(-100, Math.min(100, mouseX));
      mouseY = Math.max(-100, Math.min(100, mouseY));

      const absMouseX = Math.abs(mouseX);

      // Border 1 그라데이션 계산
      const angle1 = 135 + mouseX * 1.2;
      const opacity1First = 0.12 + absMouseX * 0.008;
      const stop1First = Math.max(10, 33 + mouseY * 0.3);
      const opacity1Second = 0.4 + absMouseX * 0.012;
      const stop1Second = Math.min(90, 66 + mouseY * 0.4);

      // Border 2 그라데이션 계산
      const angle2 = 135 + mouseX * 1.2;
      const opacity2First = 0.32 + absMouseX * 0.008;
      const stop2First = Math.max(10, 33 + mouseY * 0.3);
      const opacity2Second = 0.6 + absMouseX * 0.012;
      const stop2Second = Math.min(90, 66 + mouseY * 0.4);

      // Border 1 스타일 적용
      border1Ref.current.style.background = `linear-gradient(${angle1}deg,
          rgba(255, 255, 255, 0.0) 0%,
          rgba(255, 255, 255, ${opacity1First}) ${stop1First}%,
          rgba(255, 255, 255, ${opacity1Second}) ${stop1Second}%,
          rgba(255, 255, 255, 0.0) 100%)`;

      // Border 2 스타일 적용
      border2Ref.current.style.background = `linear-gradient(${angle2}deg,
          rgba(255, 255, 255, 0.0) 0%,
          rgba(255, 255, 255, ${opacity2First}) ${stop2First}%,
          rgba(255, 255, 255, ${opacity2Second}) ${stop2Second}%,
          rgba(255, 255, 255, 0.0) 100%)`;
    };

    // wrapper 요소에만 이벤트 등록 (전역 window X)
    wrapper.addEventListener('mouseenter', handleMouseEnter);
    wrapper.addEventListener('mouseleave', handleMouseLeave);
    wrapper.addEventListener('mousemove', handleMouseMove);

    return () => {
      wrapper.removeEventListener('mouseenter', handleMouseEnter);
      wrapper.removeEventListener('mouseleave', handleMouseLeave);
      wrapper.removeEventListener('mousemove', handleMouseMove);
    };
  }, []);

  return { wrapperRef, border1Ref, border2Ref };
}
