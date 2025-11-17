/**
 * 함수 호출을 지연시켜 성능 최적화
 * 마지막 호출 후 wait 시간이 지나야 실행됨
 *
 * @param func - 실행할 함수
 * @param wait - 대기 시간 (밀리초)
 * @returns 디바운스된 함수
 */
export function debounce<T extends (...args: unknown[]) => void>(
  func: T,
  wait: number,
): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout> | null = null;

  return function executedFunction(...args: Parameters<T>) {
    const later = () => {
      timeout = null;
      func(...args);
    };

    if (timeout) {
      clearTimeout(timeout);
    }
    timeout = setTimeout(later, wait);
  };
}
