import type { UseQueryResult, UseInfiniteQueryResult, InfiniteData } from '@tanstack/react-query';
import type { RecentNote, SearchNoteData, Note } from '@/features/main/types/search';
import { NoteItem } from '@/features/main/components/NoteItem';
import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { useInfiniteScroll } from '@/shared/hooks/useInfiniteScroll';

interface NoteListProps {
  type: 'recent' | 'search';
  recentQuery?: UseQueryResult<RecentNote[], Error>;
  searchQuery?: UseInfiniteQueryResult<InfiniteData<SearchNoteData>, Error>;
}

export function NoteList({ type, recentQuery, searchQuery }: NoteListProps) {
  const selectedIds = useSearchPanelStore((state) => state.selectedIds);
  const toggleSelection = useSearchPanelStore((state) => state.toggleSelection);
  const isDeleteMode = useSearchPanelStore((state) => state.isDeleteMode);

  // 무한 스크롤: TanStack Query useInfiniteQuery와 통합
  const { observerRef } = useInfiniteScroll({
    enabled: type === 'search',
    hasNextPage: searchQuery?.hasNextPage ?? false,
    isFetchingNextPage: searchQuery?.isFetchingNextPage ?? false,
    fetchNextPage: searchQuery?.fetchNextPage ?? (() => {}),
  });

  if (type === 'recent' && recentQuery) {
    if (recentQuery.isLoading) {
      return <LoadingSpinner />;
    }

    if (!recentQuery.data) {
      return <p className="m-0 text-center text-sm text-white/40">최근 노트가 없습니다</p>;
    }

    // recentQuery.data는 이미 RecentNote[] 배열
    const noteData = recentQuery.data;

    if (!Array.isArray(noteData) || noteData.length === 0) {
      return <p className="m-0 text-center text-sm text-white/40">최근 노트가 없습니다</p>;
    }

    return (
      <div className="w-full">
        {noteData.map((note, index) => (
          <div key={note.noteId}>
            <NoteItem
              note={note}
              isSelected={selectedIds.has(note.noteId)}
              onToggle={toggleSelection}
              isDeleteMode={isDeleteMode}
            />
            {index < noteData.length - 1 && <div className="border-b border-white/10" />}
          </div>
        ))}
      </div>
    );
  }

  if (type === 'search' && searchQuery) {
    // 로딩 중
    if (searchQuery.isLoading) {
      return <LoadingSpinner />;
    }

    // 에러 발생
    if (searchQuery.isError) {
      return <p className="m-0 text-center text-sm text-red-400">검색 에러</p>;
    }

    // 데이터 없음 (아직 로딩 전)
    if (!searchQuery.data) {
      return null;
    }

    const allNotes = searchQuery.data.pages.flatMap((page) => {
      return page.results || [];
    });

    // 검색 결과 없음
    if (allNotes.length === 0) {
      return <p className="m-0 py-8 text-center text-sm text-white/40">검색결과가 없습니다</p>;
    }

    return (
      <>
        <div className="w-full">
          {allNotes.map((note: Note, index: number) => (
            <div key={note.id}>
              <NoteItem
                note={note}
                isSelected={selectedIds.has(note.id)}
                onToggle={toggleSelection}
                isDeleteMode={isDeleteMode}
              />
              {index < allNotes.length - 1 && <div className="border-b border-white/10" />}
              {/* 마지막 아이템 또는 마지막에서 3번째 중 작은 인덱스에 배치 */}
              {index === Math.min(allNotes.length - 1, Math.max(0, allNotes.length - 3)) && (
                <div ref={observerRef} className="h-1" />
              )}
            </div>
          ))}
        </div>

        {/* 다음 페이지 로딩 중 표시 */}
        {searchQuery.isFetchingNextPage && (
          <p className="m-0 py-4 text-center text-sm text-white/60">더 불러오는 중...</p>
        )}

        {/* 마지막 페이지 도달 */}
        {!searchQuery.hasNextPage && allNotes.length > 0 && (
          <p className="m-0 py-4 text-center text-sm text-white/40">
            모든 검색 결과를 불러왔습니다
          </p>
        )}
      </>
    );
  }

  return null;
}
