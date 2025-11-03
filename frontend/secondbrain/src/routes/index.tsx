import { createFileRoute, redirect } from '@tanstack/react-router';

import { LandingPage } from '@/features/auth/pages/LandingPage';

/**
 * 랜딩 페이지 라우트 (/)
 * - 인증된 사용자는 대시보드로 자동 리다이렉트
 * - beforeLoad에서 인증 상태 체크
 */
export const Route = createFileRoute('/')({
  // 라우트 로드 전 인증 체크 (TanStack Router 공식 권장 패턴)
  beforeLoad: ({ context }) => {
    // 이미 인증된 사용자는 대시보드로 리다이렉트
    if (context.auth.isAuthenticated) {
      // eslint-disable-next-line @typescript-eslint/only-throw-error
      throw redirect({ to: '/dashboard' });
    }
  },
  component: LandingPage,
});
