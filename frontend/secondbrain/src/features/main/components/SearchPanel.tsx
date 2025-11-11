import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import { PanelHeader } from '@/features/main/components/PanelHeader';
import { NoteList } from '@/features/main/components/NoteList';
import { useRecentNotes } from '@/features/main/hooks/useRecentNotes';
import { useSearchNotes } from '@/features/main/hooks/useSearchNotes';
import { GlassContainer } from '@/shared/components/GlassContainer/GlassContainer';

export function SearchPanel() {
  const mode = useSearchPanelStore((state) => state.mode);
  const query = useSearchPanelStore((state) => state.query);

  // 실제 데이터 조회
  const recentNotesQuery = useRecentNotes();
  const searchNotesQuery = useSearchNotes({ keyword: query });

  return (
    <GlassContainer>
      <PanelHeader />
      {/* Divider between header and list */}
      <div className="border-b border-white/75" />
      <div
        data-scroll-container="true"
        className="m-0 flex flex-1 flex-col overflow-y-auto [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        {mode === 'recent' && <NoteList type="recent" recentQuery={recentNotesQuery} />}
        {mode === 'search' && <NoteList type="search" searchQuery={searchNotesQuery} />}
      </div>
    </GlassContainer>
  );
}
