import { useState } from 'react';

/**
 * API Key 표시/숨김 토글 Hook
 *
 * @returns {object} isVisible - 현재 표시 상태, toggle - 토글 함수
 *
 * @example
 * const { isVisible, toggle } = useApiKeyVisibility();
 * <button onClick={toggle}>{isVisible ? '숨기기' : '표시'}</button>
 */
export function useApiKeyVisibility() {
  const [isVisible, setIsVisible] = useState(false);

  const toggle = () => setIsVisible((prev) => !prev);

  return { isVisible, toggle };
}
