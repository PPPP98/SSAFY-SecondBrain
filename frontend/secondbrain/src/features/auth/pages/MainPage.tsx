import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { UserProfile } from '@/features/auth/components/UserProfile';
import { LogoutButton } from '@/features/auth/components/LogoutButton';
import { MainLayout } from '@/layouts/MainLayout';

/**
 * 메인 페이지
 * - 사용자 프로필 정보 표시
 * - 로그아웃 버튼 제공
 * - 로딩 및 에러 상태 처리
 * - 인증 체크는 라우트 레벨(main.tsx)에서 beforeLoad로 처리
 */
export function MainPage() {
  const { data: user, isLoading, isError } = useCurrentUser();

  if (isLoading) {
    return (
      <MainLayout>
        <div className="flex min-h-dvh items-center justify-center">
          <p>로딩 중...</p>
        </div>
      </MainLayout>
    );
  }

  if (isError || !user) {
    return (
      <MainLayout>
        <div className="flex min-h-dvh items-center justify-center">
          <p>사용자 정보를 불러올 수 없습니다.</p>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <div className="flex min-h-dvh items-center justify-center">
        <div className="flex flex-col items-center gap-6">
          <UserProfile user={user} />
          <LogoutButton variant="secondary" size="md" />
        </div>
      </div>
    </MainLayout>
  );
}
