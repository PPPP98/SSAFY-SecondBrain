import { create } from 'zustand';

interface PageCollectionStore {
  pages: Set<string>;
  addPage: (url: string) => boolean;
  removePage: (url: string) => void;
  clearPages: () => void;
  getPageList: () => string[];
  hasPage: (url: string) => boolean;
}

/**
 * Page Collection Store
 * - 여러 페이지를 수집하여 한 번에 저장하기 위한 Store
 * - Set 기반으로 중복 자동 방지
 * - Chrome Extension 환경에서 안전하게 작동
 */
export const usePageCollectionStore = create<PageCollectionStore>((set, get) => ({
  pages: new Set<string>(),

  addPage: (url: string) => {
    const { pages } = get();

    // 중복 체크
    if (pages.has(url)) {
      return false; // 이미 존재
    }

    // 새 Set 생성하여 불변성 유지
    const newPages = new Set(pages);
    newPages.add(url);
    set({ pages: newPages });

    return true; // 추가 성공
  },

  removePage: (url: string) => {
    const { pages } = get();
    const newPages = new Set(pages);
    newPages.delete(url);
    set({ pages: newPages });
  },

  clearPages: () => {
    set({ pages: new Set() });
  },

  getPageList: () => {
    return Array.from(get().pages);
  },

  hasPage: (url: string) => {
    return get().pages.has(url);
  },
}));
