import { GoogleLoginButton } from '@/features/auth/components/GoogleLoginButton';
import LandingLayout from '@/layouts/LandingLayout';

/**
 * 랜딩 페이지
 * - Google 로그인 버튼 표시
 * - 인증 체크는 라우트 레벨(index.tsx)에서 beforeLoad로 처리
 */
export function LandingPage() {
  return (
    <LandingLayout>
      <div className="text-center">
        <h1 className="mb-8 text-4xl font-bold">Welcome to Second Brain</h1>
        <GoogleLoginButton text="signin" />
      </div>
    </LandingLayout>
  );
}
