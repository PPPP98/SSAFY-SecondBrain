import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import { PanelHeader } from '@/features/main/components/PanelHeader';
import { NoteList } from '@/features/main/components/NoteList';
import { useRecentNotes } from '@/features/main/hooks/useRecentNotes';
import { useSearchNotes } from '@/features/main/hooks/useSearchNotes';
import type { RecentNote, Note } from '@/features/main/types/search';

export function SearchPanel() {
  const mode = useSearchPanelStore((state) => state.mode);
  const query = useSearchPanelStore((state) => state.query);

  // 실제 데이터 조회
  const recentNotesQuery = useRecentNotes();
  const searchNotesQuery = useSearchNotes({ keyword: query });

  // 현재 모드에 따라 전체 노트 ID 추출
  const getAllNoteIds = (): number[] => {
    if (mode === 'recent' && recentNotesQuery.data) {
      const noteData = (recentNotesQuery.data as unknown as [unknown, RecentNote[]])[1];
      return Array.isArray(noteData) ? noteData.map((note) => note.noteId) : [];
    }

    if (mode === 'search' && searchNotesQuery.data?.pages) {
      const allNotes = searchNotesQuery.data.pages.flatMap((page) => {
        const pageResults = page.results as unknown as [unknown, Note[]];
        return pageResults[1] || [];
      });
      return allNotes.map((note) => note.id);
    }

    return [];
  };

  const allNoteIds = getAllNoteIds();

  return (
    <div className="flex h-full flex-col">
      <PanelHeader allNoteIds={allNoteIds} />
      <div
        data-scroll-container="true"
        className="m-0 flex flex-1 flex-col overflow-y-auto [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        {mode === 'recent' && <NoteList type="recent" recentQuery={recentNotesQuery} />}
        {mode === 'search' && <NoteList type="search" searchQuery={searchNotesQuery} />}
      </div>
    </div>
  );
}
