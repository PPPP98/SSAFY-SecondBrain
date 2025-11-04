import { useRef, useEffect, createElement } from 'react';
import type { ComponentPropsWithoutRef, ReactNode } from 'react';
import '@/shared/styles/glass-base.css';

type ElementType = 'button' | 'input' | 'div';

type GlassElementProps<El extends ElementType> = {
  as: El;
  icon?: ReactNode;
  scale?: El extends 'input' ? 'sm' | 'md' : never;
  children?: React.ReactNode;
} & Omit<ComponentPropsWithoutRef<El>, 'as' | 'icon' | 'scale'>;

const GlassElement = <El extends ElementType>({
  as,
  icon,
  scale,
  className = '',
  children,
  ...props
}: GlassElementProps<El>) => {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const border1Ref = useRef<HTMLSpanElement>(null);
  const border2Ref = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!wrapperRef.current || !border1Ref.current || !border2Ref.current) return;

      const rect = wrapperRef.current.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;

      let mouseX = ((e.clientX - centerX) / rect.width) * 100;
      let mouseY = ((e.clientY - centerY) / rect.height) * 100;

      mouseX = Math.max(-100, Math.min(100, mouseX));
      mouseY = Math.max(-100, Math.min(100, mouseY));

      const absMouseX = Math.abs(mouseX);

      const angle1 = 135 + mouseX * 1.2;
      const opacity1First = 0.12 + absMouseX * 0.008;
      const stop1First = Math.max(10, 33 + mouseY * 0.3);
      const opacity1Second = 0.4 + absMouseX * 0.012;
      const stop1Second = Math.min(90, 66 + mouseY * 0.4);

      const angle2 = 135 + mouseX * 1.2;
      const opacity2First = 0.32 + absMouseX * 0.008;
      const stop2First = Math.max(10, 33 + mouseY * 0.3);
      const opacity2Second = 0.6 + absMouseX * 0.012;
      const stop2Second = Math.min(90, 66 + mouseY * 0.4);

      border1Ref.current.style.background = `linear-gradient(${angle1}deg,
          rgba(255, 255, 255, 0.0) 0%,
          rgba(255, 255, 255, ${opacity1First}) ${stop1First}%,
          rgba(255, 255, 255, ${opacity1Second}) ${stop1Second}%,
          rgba(255, 255, 255, 0.0) 100%)`;

      border2Ref.current.style.background = `linear-gradient(${angle2}deg,
          rgba(255, 255, 255, 0.0) 0%,
          rgba(255, 255, 255, ${opacity2First}) ${stop2First}%,
          rgba(255, 255, 255, ${opacity2Second}) ${stop2Second}%,
          rgba(255, 255, 255, 0.0) 100%)`;
    };

    window.addEventListener('mousemove', handleMouseMove);

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
    };
  }, []);

  const scaleClasses =
    as === 'button'
      ? 'p-0 text-xl'
      : as === 'input' && scale
        ? {
            sm: 'p-0 text-sm',
            md: 'p-4 px-6 text-base',
          }[scale]
        : 'p-6 px-8 text-xl';

  const sizeClasses =
    as === 'button'
      ? 'w-14 h-14 rounded-full'
      : as === 'input' && scale === 'sm'
        ? 'w-14 h-14 rounded-full'
        : as === 'input' && scale === 'md'
          ? 'w-[22rem] h-14'
          : as === 'div'
            ? 'w-[27rem]'
            : '';

  const borderRadius =
    as === 'button' || (as === 'input' && scale === 'sm') ? 'rounded-full' : 'rounded-3xl';

  const baseClassName = `backdrop-saturate-180 text-shadow-[0px_2px_12px_rgba(0,0,0,0.4)] relative ${as === 'button' ? 'flex cursor-pointer items-center justify-center' : as === 'input' ? 'block appearance-none outline-none focus:outline-none' : ''} ${borderRadius} bg-white/15 ${scaleClasses} font-medium text-white shadow-[0px_12px_40px_rgba(0,0,0,0.25)] backdrop-blur-[3.5px] ${sizeClasses}`;

  // children 렌더링 로직
  const elementChildren =
    as !== 'input' ? (
      <>
        {icon && (
          <span className={`${as !== 'button' ? 'mr-2' : ''} inline-flex items-center`}>
            {icon}
          </span>
        )}
        {children}
      </>
    ) : null;

  // 타입 안전성을 위해 각 element type별로 분기 처리
  const renderElement = () => {
    const commonProps = { className: baseClassName };

    if (as === 'button') {
      return createElement(
        'button',
        { ...props, ...commonProps } as ComponentPropsWithoutRef<'button'>,
        elementChildren,
      );
    }

    if (as === 'input') {
      const inputTypeAttr = scale === 'sm' ? 'checkbox' : scale === 'md' ? 'text' : undefined;
      return createElement('input', {
        ...props,
        ...commonProps,
        ...(inputTypeAttr && { type: inputTypeAttr }),
      } as ComponentPropsWithoutRef<'input'>);
    }

    // as === 'div'
    return createElement(
      'div',
      { ...props, ...commonProps } as ComponentPropsWithoutRef<'div'>,
      elementChildren,
    );
  };

  return (
    <div
      ref={wrapperRef}
      className={`relative ${as === 'input' ? 'w-fit' : as === 'div' ? 'w-[27rem]' : 'w-fit'} ${className}`}
    >
      {/* Glass 본체 (컨텐츠 영역) */}
      {renderElement()}

      {/* 동적 테두리 1 (mix-blend-mode: screen) */}
      <span
        ref={border1Ref}
        className={`glass-border pointer-events-none absolute inset-0 z-10 ${borderRadius} p-px opacity-20 mix-blend-screen`}
      ></span>

      {/* 동적 테두리 2 (mix-blend-mode: overlay) */}
      <span
        ref={border2Ref}
        className={`glass-border pointer-events-none absolute inset-0 z-10 ${borderRadius} p-px mix-blend-overlay`}
      ></span>
    </div>
  );
};

export default GlassElement;
