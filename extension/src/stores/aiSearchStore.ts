import { create } from 'zustand';
import type { DocumentSchema } from '@/types/aiSearch';
import type { NoteSearchResult } from '@/types/note';
import { searchWithAI, searchWithElasticsearch } from '@/services/aiSearchService';

/**
 * AI 검색 상태 관리 Store
 * - AI Agent 검색 + Elasticsearch 병렬 검색
 * - 검색 결과, 로딩, 에러 상태 관리
 */
interface AiSearchState {
  // 검색 상태
  keyword: string;
  isSearching: boolean;
  isFocused: boolean;
  esCompleted: boolean; // ES 검색 완료 여부

  // AI 검색 결과
  aiResponse: string;
  aiDocuments: DocumentSchema[];

  // Elasticsearch 결과
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
  aiResponse: string;
  aiDocuments: DocumentSchema[];
  notesList: NoteSearchResult[];
  totalCount: number;
  timestamp: number;
}

const searchCache = new Map<string, SearchCache>();
const CACHE_TTL = 5 * 60 * 1000; // 5분

export const useAiSearchStore = create<AiSearchState>((set) => ({
  // 초기 상태
  keyword: '',
  isSearching: false,
  isFocused: false,
  esCompleted: false,
  aiResponse: '',
  aiDocuments: [],
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

  // 검색 실행 (AI + Elasticsearch 병렬, Progressive Loading)
  search: async (keyword) => {
    if (!keyword.trim()) {
      return;
    }

    set({ isSearching: true, error: null, keyword: keyword.trim(), esCompleted: false });

    try {
      // 캐시 확인
      const cacheKey = keyword.toLowerCase().trim();
      const cached = searchCache.get(cacheKey);

      if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
        // 캐시된 결과 사용
        set({
          aiResponse: cached.aiResponse,
          aiDocuments: cached.aiDocuments,
          notesList: cached.notesList,
          totalCount: cached.totalCount,
          isSearching: false,
        });
        return;
      }

      // 병렬 API 호출 - 먼저 응답 온 것부터 즉시 표시!
      let aiCompleted = false;
      let esCompleted = false;

      const aiPromise = searchWithAI(keyword.trim())
        .then((aiResult) => {
          set((state) => ({
            ...state,
            aiResponse: aiResult.response,
            aiDocuments: aiResult.documents,
          }));
          aiCompleted = true;
          if (esCompleted) set({ isSearching: false });
          return aiResult;
        })
        .catch(() => {
          aiCompleted = true;
          if (esCompleted) set({ isSearching: false });
          return { response: '', documents: [], success: false };
        });

      const esPromise = searchWithElasticsearch(keyword.trim())
        .then((esResult) => {
          set((state) => ({
            ...state,
            notesList: esResult.results,
            totalCount: esResult.totalCount,
            esCompleted: true,
          }));
          esCompleted = true;
          if (aiCompleted) set({ isSearching: false });
          return esResult;
        })
        .catch(() => {
          set((state) => ({ ...state, esCompleted: true }));
          esCompleted = true;
          if (aiCompleted) set({ isSearching: false });
          return { results: [], totalCount: 0, currentPage: 0, totalPages: 0, pageSize: 0 };
        });

      // 둘 다 완료될 때까지 대기 (캐싱용)
      const [aiResult, esResult] = await Promise.all([aiPromise, esPromise]);

      // 최종 결과 (캐싱용)
      const aiResponse = 'response' in aiResult ? aiResult.response : '';
      const aiDocuments = 'documents' in aiResult ? aiResult.documents : [];
      const notesList = esResult.results;
      const totalCount = esResult.totalCount;

      // 캐시 저장
      searchCache.set(cacheKey, {
        keyword,
        aiResponse,
        aiDocuments,
        notesList,
        totalCount,
        timestamp: Date.now(),
      });

      // 오래된 캐시 정리
      for (const [key, value] of searchCache.entries()) {
        if (Date.now() - value.timestamp > CACHE_TTL) {
          searchCache.delete(key);
        }
      }

      // 로딩은 이미 개별 then/catch에서 종료됨
      // 에러는 둘 다 실패한 경우만
      if (!aiResponse && notesList.length === 0) {
        set({ error: '검색 중 오류가 발생했습니다', isSearching: false });
      }
    } catch (error) {
      console.error('[AiSearchStore] Search failed:', error);
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
      aiResponse: '',
      aiDocuments: [],
      notesList: [],
      totalCount: 0,
      error: null,
      isFocused: false,
      isSearching: false,
      esCompleted: false,
    });
  },
}));
