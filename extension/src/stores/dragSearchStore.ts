import { create } from 'zustand';
import browser from 'webextension-polyfill';
import type { NoteSearchResult } from '@/types/note';
import type { SearchHistoryItem } from '@/types/dragSearch';

interface DragSearchState {
  // 상태
  keyword: string;
  results: NoteSearchResult[];
  totalCount: number;
  isLoading: boolean;
  error: string | null;
  isVisible: boolean;
  searchHistory: SearchHistoryItem[];

  // 액션
  setSearchResults: (keyword: string, results: NoteSearchResult[], totalCount: number) => void;
  setLoading: (isLoading: boolean) => void;
  setError: (error: string) => void;
  clearSearch: () => void;
  toggleVisible: () => void;
  showPanel: () => void;
  hidePanel: () => void;

  // 히스토리 관리
  addToHistory: (keyword: string, resultCount: number) => void;
  clearHistory: () => void;
  loadHistory: () => Promise<void>;
}

/**
 * 드래그 검색 상태 관리 스토어
 * 검색 결과, 로딩, 에러, 히스토리 등 관리
 */
export const useDragSearchStore = create<DragSearchState>((set, get) => ({
  // 초기 상태
  keyword: '',
  results: [],
  totalCount: 0,
  isLoading: false,
  error: null,
  isVisible: false,
  searchHistory: [],

  // 검색 결과 설정
  setSearchResults: (keyword, results, totalCount) => {
    set({
      keyword,
      results,
      totalCount,
      isLoading: false,
      error: null,
      isVisible: true,
    });
    get().addToHistory(keyword, totalCount);
  },

  // 로딩 상태 설정
  setLoading: (isLoading) => {
    set({ isLoading, error: null });
  },

  // 에러 설정
  setError: (error) => {
    set({ error, isLoading: false, isVisible: true });
  },

  // 검색 초기화
  clearSearch: () => {
    set({
      keyword: '',
      results: [],
      totalCount: 0,
      isLoading: false,
      error: null,
    });
  },

  // 패널 토글
  toggleVisible: () => {
    set((state) => ({ isVisible: !state.isVisible }));
  },

  // 패널 표시
  showPanel: () => {
    set({ isVisible: true });
  },

  // 패널 숨기기
  hidePanel: () => {
    set({ isVisible: false });
  },

  // 히스토리 추가
  addToHistory: (keyword, resultCount) => {
    const newItem: SearchHistoryItem = {
      keyword,
      timestamp: Date.now(),
      resultCount,
    };

    set((state) => {
      // 중복 제거 및 최대 10개 유지
      const filtered = state.searchHistory.filter((item) => item.keyword !== keyword);
      const newHistory = [newItem, ...filtered].slice(0, 10);

      // chrome.storage에 저장
      browser.storage.local
        .set({ dragSearchHistory: newHistory })
        .catch((err) => console.error('[DragSearchStore] Failed to save history:', err));

      return { searchHistory: newHistory };
    });
  },

  // 히스토리 삭제
  clearHistory: () => {
    browser.storage.local
      .remove('dragSearchHistory')
      .catch((err) => console.error('[DragSearchStore] Failed to clear history:', err));
    set({ searchHistory: [] });
  },

  // 히스토리 로드
  loadHistory: async () => {
    try {
      const { dragSearchHistory } = await browser.storage.local.get(['dragSearchHistory']);
      if (dragSearchHistory && Array.isArray(dragSearchHistory)) {
        set({ searchHistory: dragSearchHistory as SearchHistoryItem[] });
      }
    } catch (error) {
      console.error('[DragSearchStore] Failed to load history:', error);
    }
  },
}));
