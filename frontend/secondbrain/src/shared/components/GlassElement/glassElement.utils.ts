import { STYLE_CONFIG } from '@/shared/components/GlassElement/glassElement.styles';

type ElementType = 'button' | 'input' | 'div';

// 유틸리티 함수: 스케일 클래스 가져오기
export function getScaleClasses(as: ElementType, scale?: 'sm' | 'md'): string {
  if (as === 'button') return STYLE_CONFIG.button.scale;
  if (as === 'input' && scale) return STYLE_CONFIG.input[scale].scale;
  return STYLE_CONFIG.div.scale;
}

// 유틸리티 함수: 사이즈 클래스 가져오기
export function getSizeClasses(as: ElementType, scale?: 'sm' | 'md'): string {
  if (as === 'button') return STYLE_CONFIG.button.size;
  if (as === 'input' && scale) return STYLE_CONFIG.input[scale].size;
  return STYLE_CONFIG.div.size;
}

// 유틸리티 함수: 테두리 반경 가져오기
export function getBorderRadius(as: ElementType, scale?: 'sm' | 'md'): string {
  if (as === 'button') return STYLE_CONFIG.button.borderRadius;
  if (as === 'input' && scale) return STYLE_CONFIG.input[scale].borderRadius;
  return STYLE_CONFIG.div.borderRadius;
}

// 유틸리티 함수: 엘리먼트별 특정 클래스 가져오기
export function getElementSpecificClasses(as: ElementType): string {
  if (as === 'button') return STYLE_CONFIG.button.elementSpecific;
  if (as === 'input') return STYLE_CONFIG.input.elementSpecific;
  return STYLE_CONFIG.div.elementSpecific;
}

// 유틸리티 함수: input type 속성 가져오기
export function getInputType(scale?: 'sm' | 'md'): 'checkbox' | 'text' | undefined {
  if (!scale) return undefined;
  return STYLE_CONFIG.input[scale].type;
}
