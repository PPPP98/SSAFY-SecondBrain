import { useEffect, useRef } from 'react';
import type { UseQueryResult, UseInfiniteQueryResult, InfiniteData } from '@tanstack/react-query';
import type { RecentNote, SearchNoteData, Note } from '@/features/main/types/search';
import { NoteItem } from '@/features/main/components/NoteItem';
import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';

interface NoteListProps {
  type: 'recent' | 'search';
  recentQuery?: UseQueryResult<RecentNote[], Error>;
  searchQuery?: UseInfiniteQueryResult<InfiniteData<SearchNoteData>, Error>;
}

export function NoteList({ type, recentQuery, searchQuery }: NoteListProps) {
  const selectedIds = useSearchPanelStore((state) => state.selectedIds);
  const toggleSelection = useSearchPanelStore((state) => state.toggleSelection);
  const observerRef = useRef<HTMLDivElement>(null); //

  useEffect(() => {
    if (type !== 'search' || !searchQuery) return;

    const { hasNextPage, isFetchingNextPage, fetchNextPage } = searchQuery;

    if (!observerRef.current) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const [entry] = entries;
        // 감시 요소가 화면에 나타나고, 다음 페이지가 있으며, 현재 로딩 중이 아닐 때
        if (entry.isIntersecting && hasNextPage && !isFetchingNextPage) {
          void fetchNextPage();
        }
      },
      {
        root: null, // 뷰포트 기준
        rootMargin: '100px', // 100px 전에 미리 로드
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
      return <p className="m-0 text-center text-sm text-white/60">로딩 중...</p>;
    }

    if (!recentQuery.data) {
      return <p className="m-0 text-center text-sm text-white/40">최근 노트가 없습니다</p>;
    }

    // 인덱스 1번에 실제 노트 데이터 배열이 있음
    const noteData = (recentQuery.data as unknown as [unknown, RecentNote[]])[1];

    if (!noteData || noteData.length === 0) {
      return <p className="m-0 text-center text-sm text-white/40">최근 노트가 없습니다</p>;
    }

    return (
      <div className="w-full space-y-4">
        {noteData.map((note) => (
          <NoteItem
            key={note.noteId}
            note={note}
            isSelected={selectedIds.has(note.noteId)}
            onToggle={toggleSelection}
          />
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
      const pageResults = page.results as unknown as [unknown, Note[]];
      return pageResults[1] || [];
    });

    if (allNotes.length === 0) {
      return null;
    }

    return (
      <>
        <div className="w-full space-y-4">
          {allNotes.map((note: Note) => (
            <NoteItem
              key={note.id}
              note={note}
              isSelected={selectedIds.has(note.id)}
              onToggle={toggleSelection}
            />
          ))}
        </div>

        {/* 무한 스크롤 감시 요소 */}
        <div ref={observerRef} className="h-4" />

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
