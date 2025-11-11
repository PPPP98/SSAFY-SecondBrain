import { create } from 'zustand';

type PanelMode = 'closed' | 'recent' | 'search';

interface SearchPanelState {
  // ===== UI 상태 =====
  mode: PanelMode /** 패널 모드: closed(닫힘) | recent(최근 노트) | search(검색 결과) */;
  query: string /** 검색 쿼리 */;
  isOpen: boolean /** 패널이 열려있는지 여부 (computed) */;

  // ===== 선택 상태 =====
  selectedIds: Set<number> /** 선택된 노트 ID 집합 */;
  isSelectAllMode: boolean /** 전체 선택 모드 여부 */;

  // ===== 그래프 강조 상태 =====
  highlightedNodeIds: Set<number> /** 검색 결과로 강조할 노드 ID 집합 */;

  // ===== 패널 제어 액션 =====
  openRecent: () => void /** 패널 열기 - 최근 노트 표시 */;
  startSearch: (query: string) => void /** 검색 시작 - 검색 결과 표시 */;
  closePanel: () => void /** 패널 닫기 */;
  updateQuery: (query: string) => void /** 검색 쿼리 업데이트 (자동 모드 전환) */;

  // ===== 선택 관리 액션 =====
  toggleSelection: (id: number) => void /** 개별 노트 선택/해제 토글 */;
  selectAll: (ids: number[]) => void /** 전체 선택 */;
  deselectAll: () => void /** 전체 선택 해제 */;
  clearSelection: () => void /** 선택 상태 초기화 */;

  // ===== 그래프 강조 액션 =====
  setHighlightedNodes: (ids: number[]) => void /** 강조할 노드 ID 설정 */;
  clearHighlightedNodes: () => void /** 강조 상태 초기화 */;

  hasSelection: () => boolean /** 선택된 항목이 있는지 확인 */;

  reset: () => void /** 전체 상태 초기화 */;
}

export const useSearchPanelStore = create<SearchPanelState>((set, get) => ({
  // ===== 초기 상태 =====
  mode: 'closed',
  query: '',
  isOpen: false,
  selectedIds: new Set<number>(),
  isSelectAllMode: false,
  highlightedNodeIds: new Set<number>(),

  // ===== 패널 제어 액션 구현 =====
  openRecent: () =>
    set({
      mode: 'recent',
      query: '',
      isOpen: true,
    }),

  startSearch: (query: string) =>
    set({
      mode: 'search',
      query,
      isOpen: true,
    }),

  closePanel: () =>
    set({
      mode: 'closed',
      query: '',
      isOpen: false,
      selectedIds: new Set<number>(),
      isSelectAllMode: false,
      highlightedNodeIds: new Set<number>(),
    }),

  updateQuery: (query: string) => {
    const trimmed = query.trim();
    set({
      query,
      mode: trimmed ? 'search' : 'recent',
      isOpen: true,
    });
  },

  // ===== 선택 관리 액션 구현 =====
  toggleSelection: (id: number) => {
    const { selectedIds } = get();
    const newSelectedIds = new Set(selectedIds);

    if (newSelectedIds.has(id)) {
      newSelectedIds.delete(id);
    } else {
      newSelectedIds.add(id);
    }

    set({ selectedIds: newSelectedIds });
  },

  selectAll: (ids: number[]) =>
    set({
      selectedIds: new Set(ids),
      isSelectAllMode: true,
    }),

  deselectAll: () =>
    set({
      selectedIds: new Set<number>(),
      isSelectAllMode: false,
    }),

  clearSelection: () =>
    set({
      selectedIds: new Set<number>(),
      isSelectAllMode: false,
    }),

  // ===== 그래프 강조 액션 구현 =====
  setHighlightedNodes: (ids: number[]) =>
    set({
      highlightedNodeIds: new Set(ids),
    }),

  clearHighlightedNodes: () =>
    set({
      highlightedNodeIds: new Set<number>(),
    }),

  // ===== 계산된 값 구현 =====
  hasSelection: () => get().selectedIds.size > 0,

  reset: () =>
    set({
      mode: 'closed',
      query: '',
      isOpen: false,
      selectedIds: new Set<number>(),
      isSelectAllMode: false,
      highlightedNodeIds: new Set<number>(),
    }),
}));
