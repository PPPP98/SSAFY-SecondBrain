import { create } from 'zustand';
import * as storage from '@/services/storageService';
import type { PendingTextSnippet } from '@/types/pendingTextSnippet';

interface PendingTextSnippetsStore {
  snippets: PendingTextSnippet[];
  isLoading: boolean;
  error: string | null;

  // 초기화 (컴포넌트 마운트 시 호출)
  initialize: () => Promise<void>;

  // 비동기 Actions
  addSnippet: (snippet: PendingTextSnippet) => Promise<boolean>;
  removeSnippet: (id: string) => Promise<void>;
  clearSnippets: () => Promise<void>;

  // Storage 동기화 (storage.onChanged용)
  syncFromStorage: (snippets: PendingTextSnippet[]) => void;

  // Getters (동기)
  getSnippetList: () => PendingTextSnippet[];
  getSnippetCount: () => number;
}

/**
 * Pending Text Snippets Store (chrome.storage 기반)
 * - chrome.storage.local로 영구 저장
 * - 탭 간 상태 공유
 * - 페이지 새로고침 후에도 유지
 * - Optimistic Update 패턴으로 즉각적인 UI 반응
 */
export const usePendingTextSnippetsStore = create<PendingTextSnippetsStore>((set, get) => ({
  snippets: [],
  isLoading: false,
  error: null,

  /**
   * Store 초기화
   * - chrome.storage에서 저장된 텍스트 조각 목록 불러오기
   * - 컴포넌트 마운트 시 한 번만 호출
   */
  initialize: async () => {
    set({ isLoading: true, error: null });
    try {
      const savedSnippets = await storage.loadPendingSnippets();
      set({ snippets: savedSnippets, isLoading: false });
    } catch (error) {
      console.error('[PendingTextSnippetsStore] Failed to initialize:', error);
      set({
        error: 'Failed to load snippets',
        isLoading: false,
      });
    }
  },

  /**
   * 텍스트 조각 추가 (Optimistic Update)
   * 1. UI 즉시 업데이트 (동기)
   * 2. Storage 저장 (비동기)
   * 3. 실패 시 롤백
   */
  addSnippet: async (snippet: PendingTextSnippet) => {
    const { snippets } = get();

    // 중복 체크 (텍스트 기반)
    const duplicate = snippets.some(
      (s) => s.text === snippet.text && s.sourceUrl === snippet.sourceUrl,
    );
    if (duplicate) {
      return false;
    }

    // 1단계: UI 즉시 업데이트 (Optimistic)
    const newSnippets = [...snippets, snippet];
    set({ snippets: newSnippets, error: null });

    // 2단계: Storage 저장 (비동기)
    try {
      await storage.addPendingSnippet(snippet);
      return true;
    } catch (error) {
      // 3단계: 실패 시 롤백
      console.error('[PendingTextSnippetsStore] Failed to save snippet:', error);
      set({
        snippets, // 원래 상태로 복원
        error: 'Failed to save snippet',
      });
      return false;
    }
  },

  /**
   * 텍스트 조각 제거 (Optimistic Update)
   */
  removeSnippet: async (id: string) => {
    const { snippets } = get();

    // Optimistic Update
    const newSnippets = snippets.filter((s) => s.id !== id);
    set({ snippets: newSnippets, error: null });

    // Storage 저장
    try {
      await storage.removePendingSnippet(id);
    } catch (error) {
      console.error('[PendingTextSnippetsStore] Failed to remove snippet:', error);
      // 롤백
      set({
        snippets,
        error: 'Failed to remove snippet',
      });
    }
  },

  /**
   * 모든 텍스트 조각 삭제 (Optimistic Update)
   */
  clearSnippets: async () => {
    const oldSnippets = get().snippets;

    // Optimistic Update
    set({ snippets: [], error: null });

    // Storage 저장
    try {
      await storage.clearPendingSnippets();
    } catch (error) {
      console.error('[PendingTextSnippetsStore] Failed to clear snippets:', error);
      // 롤백
      set({
        snippets: oldSnippets,
        error: 'Failed to clear snippets',
      });
    }
  },

  /**
   * Storage 변경 동기화 (storage.onChanged 리스너에서 호출)
   * - 다른 탭에서 변경된 내용을 현재 탭에 반영
   */
  syncFromStorage: (snippets: PendingTextSnippet[]) => {
    set({ snippets });
  },

  /**
   * Getters (동기)
   */
  getSnippetList: () => {
    return get().snippets;
  },

  getSnippetCount: () => {
    return get().snippets.length;
  },
}));
