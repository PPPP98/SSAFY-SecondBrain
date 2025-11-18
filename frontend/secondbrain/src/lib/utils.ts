import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

/**
 * Tailwind CSS 클래스 병합 유틸리티
 * clsx와 tailwind-merge를 결합하여 조건부 클래스와 Tailwind 클래스 충돌 해결
 *
 * @param inputs - 클래스 값들 (문자열, 객체, 배열 등)
 * @returns 병합된 클래스 문자열
 *
 * @example
 * cn('px-2 py-1', 'px-4') // 'py-1 px-4' (px-2가 px-4로 대체됨)
 * cn('text-red-500', condition && 'text-blue-500') // 조건부 클래스
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
