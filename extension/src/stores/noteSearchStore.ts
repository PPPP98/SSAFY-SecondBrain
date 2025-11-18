import { create } from 'zustand';
import type { NoteSearchResult } from '@/types/note';
import { searchNotes } from '@/services/noteSearchService';

/**
 * 노트 검색 상태 관리 Store
 * - Elasticsearch 검색
 * - 검색 결과, 로딩, 에러 상태 관리
 */
interface NoteSearchState {
  // 검색 상태
  keyword: string;
  isSearching: boolean;
  isFocused: boolean;

  // 검색 결과
  notesList: NoteSearchResult[];
  totalCount: number;

  // 에러
  error: string | null;

  // 액션
  setFocused: (focused: boolean) => void;
  search: (keyword: string) => Promise<void>;
  clearSearch: () => void;
  setKeyword: (keyword: string) => void;
}

// 검색 결과 캐시 (메모리)
interface SearchCache {
  keyword: string;
  notesList: NoteSearchResult[];
  totalCount: number;
  timestamp: number;
}

const searchCache = new Map<string, SearchCache>();
const CACHE_TTL = 5 * 60 * 1000; // 5분

export const useNoteSearchStore = create<NoteSearchState>((set) => ({
  // 초기 상태
  keyword: '',
  isSearching: false,
  isFocused: false,
  notesList: [],
  totalCount: 0,
  error: null,

  // Focus 상태 설정
  setFocused: (focused) => {
    set({ isFocused: focused });
  },

  // 키워드 설정
  setKeyword: (keyword) => {
    set({ keyword });
  },

  // 검색 실행
  search: async (keyword) => {
    if (!keyword.trim()) {
      return;
    }

    set({ isSearching: true, error: null, keyword: keyword.trim() });

    try {
      // 캐시 확인
      const cacheKey = keyword.toLowerCase().trim();
      const cached = searchCache.get(cacheKey);

      if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
        // 캐시된 결과 사용
        set({
          notesList: cached.notesList,
          totalCount: cached.totalCount,
          isSearching: false,
        });
        return;
      }

      // API 호출
      const result = await searchNotes(keyword.trim());

      // 결과 설정
      set({
        notesList: result.results,
        totalCount: result.totalCount,
        isSearching: false,
      });

      // 캐시 저장
      searchCache.set(cacheKey, {
        keyword,
        notesList: result.results,
        totalCount: result.totalCount,
        timestamp: Date.now(),
      });

      // 오래된 캐시 정리
      for (const [key, value] of searchCache.entries()) {
        if (Date.now() - value.timestamp > CACHE_TTL) {
          searchCache.delete(key);
        }
      }

      // 검색 결과 없음
      if (result.results.length === 0) {
        set({ error: null, isSearching: false });
      }
    } catch (error) {
      console.error('[NoteSearchStore] Search failed:', error);
      set({
        error: error instanceof Error ? error.message : '검색 실패',
        isSearching: false,
      });
    }
  },

  // 검색 초기화
  clearSearch: () => {
    set({
      keyword: '',
      notesList: [],
      totalCount: 0,
      error: null,
      isFocused: false,
      isSearching: false,
    });
  },
}));
