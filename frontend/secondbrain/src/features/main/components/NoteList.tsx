import { useEffect, useRef } from 'react';
import type { UseQueryResult, UseInfiniteQueryResult, InfiniteData } from '@tanstack/react-query';
import type { RecentNote, SearchNoteData, Note } from '@/features/main/types/search';
import { NoteItem } from '@/features/main/components/NoteItem';
import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';

interface NoteListProps {
  type: 'recent' | 'search';
  recentQuery?: UseQueryResult<RecentNote[], Error>;
  searchQuery?: UseInfiniteQueryResult<InfiniteData<SearchNoteData>, Error>;
}

export function NoteList({ type, recentQuery, searchQuery }: NoteListProps) {
  const selectedIds = useSearchPanelStore((state) => state.selectedIds);
  const toggleSelection = useSearchPanelStore((state) => state.toggleSelection);
  const observerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (type !== 'search' || !searchQuery) return;

    const { hasNextPage, isFetchingNextPage, fetchNextPage } = searchQuery;

    if (!observerRef.current) return;

    // SearchPanel의 스크롤 컨테이너를 찾아서 root로 설정
    const scrollContainer = observerRef.current.closest(
      '[data-scroll-container="true"]',
    ) as HTMLElement;

    const observer = new IntersectionObserver(
      (entries) => {
        const [entry] = entries;
        // 감시 요소가 화면에 나타나고, 다음 페이지가 있으며, 현재 로딩 중이 아닐 때
        if (entry.isIntersecting && hasNextPage && !isFetchingNextPage) {
          void fetchNextPage();
        }
      },
      {
        root: scrollContainer, // SearchPanel의 스크롤 컨테이너를 기준으로
        rootMargin: '0px', // 실제로 화면에 보일 때만 로드
        threshold: 0.1,
      },
    );

    observer.observe(observerRef.current);

    return () => {
      observer.disconnect();
    };
  }, [type, searchQuery]);

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
            />
            {index < noteData.length - 1 && <div className="border-b border-white/10" />}
          </div>
        ))}
      </div>
    );
  }

  if (type === 'search' && searchQuery) {
    if (searchQuery.isError) {
      return <p className="m-0 text-center text-sm text-red-400">검색 에러</p>;
    }

    if (!searchQuery.data) {
      return null;
    }

    const allNotes = searchQuery.data.pages.flatMap((page) => {
      return page.results || [];
    });

    if (allNotes.length === 0) {
      return null;
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
