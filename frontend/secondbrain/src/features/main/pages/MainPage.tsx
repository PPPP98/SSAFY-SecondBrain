import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import MainLayout from '@/layouts/MainLayout';
import { Graph } from '@/features/main/components/Graph';
import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import { SearchPanel } from '@/features/main/components/SearchPanel';

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
    <div>
      <MainLayout>
        <div className="relative size-full">
          <Graph />
        </div>
      </MainLayout>
      <div className="absolute left-10 top-10 z-40 h-[calc(100%-5rem)] w-[27%] bg-transparent">
        {isOpen && <SearchPanel />}
      </div>
    </div>
  );
}
