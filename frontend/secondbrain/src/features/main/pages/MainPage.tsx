import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import MainLayout from '@/layouts/MainLayout';
import { Graph } from '@/features/main/components/Graph';
import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';

/**
 * 메인 페이지
 * - 사용자 프로필 정보 표시
 * - 로그아웃 버튼 제공
 * - 로딩 및 에러 상태 처리
 * - 인증 체크는 라우트 레벨(main.tsx)에서 beforeLoad로 처리
 */
export function MainPage() {
  const { data: user, isLoading, isError } = useCurrentUser();
  const isOpen = useSearchPanelStore((state) => state.isOpen);

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
      <div className="relative size-full">
        {/* Graph 영역 - 항상 전체 크기 유지 */}
        <div className="size-full">
          <Graph />
        </div>

        {/* 검색 패널 영역 - 오버레이 (왼쪽) */}
        {isOpen && (
          <div className="absolute left-0 top-0 z-40 h-full w-96 border-r border-white/20 bg-black/30 p-4 backdrop-blur-sm">
            <p className="text-white">SearchPanel 영역 (테스트용)</p>
          </div>
        )}
      </div>
    </MainLayout>
  );
}
