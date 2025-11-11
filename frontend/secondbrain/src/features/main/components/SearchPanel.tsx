import { useEffect } from 'react';
import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import { PanelHeader } from '@/features/main/components/PanelHeader';
import { NoteList } from '@/features/main/components/NoteList';
import { useRecentNotes } from '@/features/main/hooks/useRecentNotes';
import { useSearchNotes } from '@/features/main/hooks/useSearchNotes';
import { GlassContainer } from '@/shared/components/GlassContainer/GlassContainer';

export function SearchPanel() {
  const mode = useSearchPanelStore((state) => state.mode);
  const query = useSearchPanelStore((state) => state.query);
  const setHighlightedNodes = useSearchPanelStore((state) => state.setHighlightedNodes);
  const clearHighlightedNodes = useSearchPanelStore((state) => state.clearHighlightedNodes);

  // 전체 데이터 조회 (NoteList 컴포넌트용)
  const recentNotesQuery = useRecentNotes();
  const searchNotesQuery = useSearchNotes({ keyword: query });

  // 노트 ID만 추출 (React Query의 select를 통해 메모이제이션)
  const { data: searchNoteIds = [] } = useSearchNotes({
    keyword: query,
    select: (data) =>
      data.pages.flatMap((page) => page.results?.map((note) => note.id) ?? []) ?? [],
  });

  // 검색 모드일 때 검색 결과 노드를 그래프에서 강조
  useEffect(() => {
    if (mode === 'search' && searchNoteIds.length > 0) {
      setHighlightedNodes(searchNoteIds);
    } else {
      clearHighlightedNodes();
    }
  }, [mode, searchNoteIds, setHighlightedNodes, clearHighlightedNodes]);

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
