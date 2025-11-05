import { createElement } from 'react';
import type { ComponentPropsWithoutRef, ReactNode } from 'react';
import '@/shared/styles/glass-base.css';
import {
  getScaleClasses,
  getSizeClasses,
  getBorderRadius,
  getElementSpecificClasses,
  getInputType,
} from '@/shared/components/GlassElement/glassElement.utils';
import { useGlassEffect } from '@/shared/components/GlassElement/useGlassEffect';

type ElementType = 'button' | 'input' | 'div';

export type GlassElementProps<El extends ElementType> = {
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
  // 커스텀 훅으로 glass 효과 이벤트 핸들링
  const { wrapperRef, border1Ref, border2Ref } = useGlassEffect();

  // 유틸리티 함수로 스타일 가져오기
  const scaleClasses = getScaleClasses(as, scale);
  const sizeClasses = getSizeClasses(as, scale);
  const borderRadius = getBorderRadius(as, scale);
  const elementSpecific = getElementSpecificClasses(as);

  // 기본 스타일 (모든 엘리먼트 공통)
  const baseStyles = 'backdrop-saturate-180 text-shadow-[0px_2px_12px_rgba(0,0,0,0.4)] relative';
  const glassStyles =
    'bg-white/15 font-medium text-white shadow-[0px_12px_40px_rgba(0,0,0,0.25)] backdrop-blur-[3.5px]';

  // 최종 className 조합 (명확하고 가독성 높은 구조)
  const baseClassName = `${baseStyles} ${elementSpecific} ${borderRadius} ${glassStyles} ${scaleClasses} ${sizeClasses} ${className}`;

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
      const inputType = getInputType(scale);
      return createElement('input', {
        ...props,
        ...commonProps,
        ...(inputType && { type: inputType }),
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
      className={`relative ${as === 'input' ? 'w-fit' : as === 'div' ? '' : 'w-fit'}`}
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

export { GlassElement };
