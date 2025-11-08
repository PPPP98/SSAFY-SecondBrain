import { useEffect, useState } from 'react';

/**
 * 값의 변경을 지연시켜 반환하는 디바운싱 훅
 * @param value - 디바운싱할 값
 * @param delay - 지연 시간 (ms), 기본값 300ms
 * @returns 디바운싱된 값
 */
export function useDebounce<Value>(value: Value, delay: number = 300): Value {
  const [debouncedValue, setDebouncedValue] = useState<Value>(value);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      clearTimeout(handler);
    };
  }, [value, delay]);

  return debouncedValue;
}
