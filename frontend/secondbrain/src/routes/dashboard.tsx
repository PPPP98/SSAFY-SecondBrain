import { createFileRoute, redirect } from '@tanstack/react-router';

import { DashboardPage } from '@/features/auth/pages/DashboardPage';

/**
 * 대시보드 라우트 (/dashboard)
 * - 보호된 라우트 (인증 필요)
 * - beforeLoad에서 인증 체크 및 리다이렉트
 * - 컴포넌트 렌더링 전에 인증 상태 검증
 */
export const Route = createFileRoute('/dashboard')({
  // 라우트 로드 전 인증 체크 (TanStack Router 공식 권장 패턴)
  beforeLoad: ({ context }) => {
    // 미인증 사용자는 랜딩페이지로 리다이렉트
    if (!context.auth.isAuthenticated) {
      // eslint-disable-next-line @typescript-eslint/only-throw-error
      throw redirect({ to: '/' });
    }
  },
  component: DashboardPage,
});
