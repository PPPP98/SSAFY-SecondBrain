import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import { PanelHeader } from '@/features/main/components/PanelHeader';
import { NoteList } from '@/features/main/components/NoteList';
import { useRecentNotes } from '@/features/main/hooks/useRecentNotes';
import { useSearchNotes } from '@/features/main/hooks/useSearchNotes';
import '@/shared/styles/glass-base.css';

export function SearchPanel() {
  const mode = useSearchPanelStore((state) => state.mode);
  const query = useSearchPanelStore((state) => state.query);

  // 실제 데이터 조회
  const recentNotesQuery = useRecentNotes();
  const searchNotesQuery = useSearchNotes({ keyword: query });

  return (
    <div className="backdrop-saturate-180 relative flex h-full flex-col rounded-3xl bg-white/15 p-6 font-medium text-white shadow-[0px_12px_40px_rgba(0,0,0,0.25)] backdrop-blur-[3.5px]">
      {/* Glass border effects */}
      <span className="glass-border pointer-events-none absolute inset-0 z-10 rounded-3xl p-px opacity-20 mix-blend-screen"></span>
      <span className="glass-border pointer-events-none absolute inset-0 z-10 rounded-3xl p-px mix-blend-overlay"></span>

      {/* Content */}
      <div className="relative z-20 flex h-full flex-col">
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
      </div>
    </div>
  );
}
