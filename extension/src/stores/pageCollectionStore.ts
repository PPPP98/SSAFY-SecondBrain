import { create } from 'zustand';
import * as storage from '@/services/storageService';

interface PageCollectionStore {
  pages: Set<string>;
  isLoading: boolean;
  error: string | null;

  // 초기화 (컴포넌트 마운트 시 호출)
  initialize: () => Promise<void>;

  // 비동기 Actions
  addPage: (url: string) => Promise<boolean>;
  removePage: (url: string) => Promise<void>;
  clearPages: () => Promise<void>;

  // Storage 동기화 (storage.onChanged용)
  syncFromStorage: (pages: string[]) => void;

  // Getters (동기)
  getPageList: () => string[];
  hasPage: (url: string) => boolean;
  getPageCount: () => number;
}

/**
 * Page Collection Store (chrome.storage 기반)
 * - chrome.storage.local로 영구 저장
 * - 탭 간 상태 공유
 * - 페이지 새로고침 후에도 유지
 * - Optimistic Update 패턴으로 즉각적인 UI 반응
 */
export const usePageCollectionStore = create<PageCollectionStore>((set, get) => {
  // Store 생성 시 자동 초기화 (IIFE)
  void (async () => {
    try {
      const savedPages = await storage.loadCollectedPages();
      set({ pages: new Set(savedPages), isLoading: false });
    } catch (error) {
      console.error('[PageCollectionStore] Failed to auto-initialize:', error);
      set({ error: 'Failed to load pages', isLoading: false });
    }
  })();

  // Storage 변경 감지 (자동 등록)
  storage.watchStorageChanges((key, newValue) => {
    if (key === storage.STORAGE_KEYS.COLLECTED_PAGES) {
      set({ pages: new Set(newValue as string[]) });
    }
  });

  return {
    pages: new Set<string>(),
    isLoading: true, // 초기화 중
    error: null,

    /**
     * Store 초기화 (레거시 호환용 - 실제로는 자동 초기화됨)
     */
    initialize: async () => {
      // 이미 자동 초기화되므로 아무것도 안 함
      // 레거시 호출과의 호환성 유지
    },

    /**
     * 페이지 추가 (Optimistic Update)
     * 1. UI 즉시 업데이트 (동기)
     * 2. Storage 저장 (비동기)
     * 3. 실패 시 롤백
     */
    addPage: async (url: string) => {
      const { pages } = get();

      // 중복 체크
      if (pages.has(url)) {
        return false;
      }

      // 1단계: UI 즉시 업데이트 (Optimistic)
      const newPages = new Set(pages);
      newPages.add(url);
      set({ pages: newPages, error: null });

      // 2단계: Storage 저장 (비동기)
      try {
        await storage.addCollectedPage(url);
        return true;
      } catch (error) {
        // 3단계: 실패 시 롤백
        console.error('[PageCollectionStore] Failed to save page:', error);
        set({
          pages, // 원래 상태로 복원
          error: 'Failed to save page',
        });
        return false;
      }
    },

    /**
     * 페이지 제거 (Optimistic Update)
     */
    removePage: async (url: string) => {
      const { pages } = get();

      // Optimistic Update
      const newPages = new Set(pages);
      newPages.delete(url);
      set({ pages: newPages, error: null });

      // Storage 저장
      try {
        await storage.removeCollectedPage(url);
      } catch (error) {
        console.error('[PageCollectionStore] Failed to remove page:', error);
        // 롤백
        set({
          pages,
          error: 'Failed to remove page',
        });
      }
    },

    /**
     * 모든 페이지 삭제 (Optimistic Update)
     */
    clearPages: async () => {
      const oldPages = get().pages;

      // Optimistic Update
      set({ pages: new Set(), error: null });

      // Storage 저장
      try {
        await storage.clearCollectedPages();
      } catch (error) {
        console.error('[PageCollectionStore] Failed to clear pages:', error);
        // 롤백
        set({
          pages: oldPages,
          error: 'Failed to clear pages',
        });
      }
    },

    /**
     * Storage 변경 동기화 (storage.onChanged 리스너에서 호출)
     * - 다른 탭에서 변경된 내용을 현재 탭에 반영
     */
    syncFromStorage: (pages: string[]) => {
      set({ pages: new Set(pages) });
    },

    /**
     * Getters (동기)
     */
    getPageList: () => {
      return Array.from(get().pages);
    },

    hasPage: (url: string) => {
      return get().pages.has(url);
    },

    getPageCount: () => {
      return get().pages.size;
    },
  };
});
