import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import { PanelHeader } from '@/features/main/components/PanelHeader';
import { NoteList } from './NoteList';
import { useRecentNotes } from '@/features/main/hooks/useRecentNotes';
import { useSearchNotes } from '@/features/main/hooks/useSearchNotes';

export function SearchPanel() {
  const mode = useSearchPanelStore((state) => state.mode);
  const query = useSearchPanelStore((state) => state.query);

  // 실제 데이터 조회
  const recentNotesQuery = useRecentNotes();
  const searchNotesQuery = useSearchNotes({ keyword: query });

  return (
    <>
      <PanelHeader />
      <div className="flex-1 overflow-y-auto p-4 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {mode === 'recent' && <NoteList type="recent" recentQuery={recentNotesQuery} />}
        {mode === 'search' && <NoteList type="search" searchQuery={searchNotesQuery} />}
      </div>
    </>
  );
}
