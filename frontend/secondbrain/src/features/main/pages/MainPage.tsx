import { useNavigate } from '@tanstack/react-router';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { Route } from '@/routes/main';
import { MainLayout } from '@/layouts/MainLayout';
import { Graph } from '@/features/main/components/Graph';
import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import { SearchPanel } from '@/features/main/components/SearchPanel';
import { DraftEditor } from '@/features/note/components/DraftEditor';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';

/**
 * 메인 페이지
 * - 로딩 및 에러 상태 처리
 * - 인증 체크는 라우트 레벨(main.tsx)에서 beforeLoad로 처리
 * - Search Params 기반 Side Peek (Draft/Note)
 */
export function MainPage() {
  const { data: user, isLoading, isError } = useCurrentUser();
  const navigate = useNavigate({ from: Route.fullPath });
  const search = Route.useSearch();
  const isOpen = useSearchPanelStore((state) => state.isOpen);

  // PlusIcon 클릭: Draft 생성
  const handleCreateDraft = () => {
    const draftId = crypto.randomUUID();
    void navigate({ search: { draft: draftId } });
  };

  // Side Peek 닫기
  const handleCloseSidePeek = () => {
    void navigate({ search: {} });
  };

  if (isLoading) {
    return (
      <MainLayout onPlusClick={handleCreateDraft}>
        <LoadingSpinner />
      </MainLayout>
    );
  }

  if (isError || !user) {
    return (
      <MainLayout onPlusClick={handleCreateDraft}>
        <div className="flex min-h-dvh items-center justify-center">
          <p>사용자 정보를 불러올 수 없습니다.</p>
        </div>
      </MainLayout>
    );
  }

  return (
    <div>
      <MainLayout onPlusClick={handleCreateDraft}>
        {/* 배경: Graph */}
        <Graph />

        {/* Side Peek: Draft */}
        {search.draft && (
          <DraftEditor
            draftId={search.draft}
            isOpen={!!search.draft}
            onClose={handleCloseSidePeek}
          />
        )}

        {/* Side Peek: Note (Phase 3) */}
        {/* {search.noteId && <SidePeekNote noteId={search.noteId} />} */}
      </MainLayout>
      <div
        className={`absolute left-10 top-10 z-40 h-[calc(100%-5rem)] w-[27%] bg-transparent transition-all duration-200 ease-out motion-reduce:transition-none ${
          isOpen
            ? 'pointer-events-auto translate-y-0 scale-100 opacity-100'
            : 'pointer-events-none -translate-y-2 scale-95 opacity-0'
        }`}
      >
        <SearchPanel />
      </div>
    </div>
  );
}
