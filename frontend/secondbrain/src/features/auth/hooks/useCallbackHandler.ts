import { useEffect, useRef } from 'react';
import { useNavigate } from '@tanstack/react-router';

/**
 * OAuth2 콜백 처리 커스텀 훅
 * - URL에서 code 파라미터 추출 및 검증
 * - code를 JWT 토큰으로 교환
 * - 에러 처리 및 리다이렉트
 *
 * useEffect 로직을 커스텀 훅으로 분리하여 재사용성 향상
 */
export function useCallbackHandler(
  code: string | undefined,
  error: string | undefined,
  exchangeToken: (code: string) => void,
) {
  const navigate = useNavigate();
  const hasProcessed = useRef(false);

  useEffect(() => {
    // 이미 처리된 경우 중복 실행 방지 (React Strict Mode 대응)
    if (hasProcessed.current) {
      return;
    }

    // 에러 파라미터가 있으면 즉시 랜딩페이지로 이동
    if (error) {
      hasProcessed.current = true;
      void navigate({ to: '/', search: { error: 'oauth_error' } });
      return;
    }

    // code 파라미터가 없으면 에러 처리
    if (!code) {
      hasProcessed.current = true;
      void navigate({ to: '/', search: { error: 'missing_code' } });
      return;
    }

    // code를 JWT 토큰으로 교환
    hasProcessed.current = true;
    exchangeToken(code);
  }, [code, error, exchangeToken, navigate]);
}
