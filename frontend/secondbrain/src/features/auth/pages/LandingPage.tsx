import { GoogleLoginButton } from '@/features/auth/components/GoogleLoginButton';

/**
 * 랜딩 페이지
 * - Google 로그인 버튼 표시
 * - 인증 체크는 라우트 레벨(index.tsx)에서 beforeLoad로 처리
 */
export function LandingPage() {
  return (
    <div className="flex min-h-dvh items-center justify-center">
      <div className="text-center">
        <h1 className="mb-8 text-4xl font-bold">Welcome to Second Brain</h1>
        <GoogleLoginButton text="signin" />
      </div>
    </div>
  );
}
