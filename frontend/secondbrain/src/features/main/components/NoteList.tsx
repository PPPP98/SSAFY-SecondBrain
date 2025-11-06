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

  if (type === 'recent' && recentQuery) {
    if (recentQuery.isLoading) {
      return <p className="text-center text-sm text-white/60">로딩 중...</p>;
    }

    if (recentQuery.isError) {
      return <p className="text-center text-sm text-red-400">에러가 발생했습니다</p>;
    }

    if (!recentQuery.data) {
      return <p className="text-center text-sm text-white/40">최근 노트가 없습니다</p>;
    }

    // 인덱스 1번에 실제 노트 데이터 배열이 있음
    const noteData = (recentQuery.data as unknown as [unknown, RecentNote[]])[1];

    if (!noteData || noteData.length === 0) {
      return <p className="text-center text-sm text-white/40">최근 노트가 없습니다</p>;
    }

    return (
      <div className="space-y-2">
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
      return <p className="text-center text-sm text-red-400">검색 에러</p>;
    }

    if (!searchQuery.data) {
      return <p className="text-center text-sm text-yellow-400">검색어를 입력하세요</p>;
    }

    const allNotes = searchQuery.data.pages.flatMap((page) => {
      const pageResults = page.results as unknown as [unknown, Note[]];
      return pageResults[1] || [];
    });

    if (allNotes.length === 0) {
      return <p className="text-center text-sm text-white/40">검색 결과가 없습니다</p>;
    }

    return (
      <div className="space-y-2">
        {allNotes.map((note: Note) => (
          <NoteItem
            key={note.id}
            note={note}
            isSelected={selectedIds.has(note.id)}
            onToggle={toggleSelection}
          />
        ))}
      </div>
    );
  }

  return null;
}
