import { create } from 'zustand';
import type { SaveRequest, SaveRequestStatus } from '@/types/save';
import * as storage from '@/services/storageService';

interface SaveStatusStore {
  /** 진행 중인 저장 요청 맵 (id → SaveRequest) */
  saveRequests: Map<string, SaveRequest>;

  /**
   * Store 초기화
   * - chrome.storage에서 저장된 요청 목록 불러오기
   * - 컴포넌트 마운트 시 한 번만 호출
   */
  initialize: () => Promise<void>;

  /**
   * Storage 동기화 (storage.onChanged용)
   * - 다른 탭에서 변경된 내용을 현재 탭에 반영
   */
  syncFromStorage: (requests: SaveRequest[]) => void;

  /**
   * 새 저장 요청 추가
   * @param url 저장할 URL
   * @param batchId 배치 ID
   * @param batchTimestamp 배치 타임스탬프
   * @returns 생성된 요청 ID
   */
  addSaveRequest: (url: string, batchId: string, batchTimestamp: number) => string;

  /**
   * 저장 상태 업데이트
   * @param id 요청 ID
   * @param status 새로운 상태
   * @param error 에러 메시지 (선택적)
   */
  updateSaveStatus: (id: string, status: SaveRequestStatus, error?: string) => void;

  /**
   * 저장 요청 제거
   * @param id 요청 ID
   */
  removeSaveRequest: (id: string) => void;

  /**
   * 완료된 요청 일괄 삭제
   */
  clearCompletedRequests: () => void;

  /**
   * 현재 저장 중인 요청 개수
   */
  getSavingCount: () => number;

  /**
   * 요청 목록 배열로 반환 (최신순)
   */
  getRequestList: () => SaveRequest[];

  /**
   * 외부(다른 탭)에서 저장 시작 브로드캐스트 받음
   * @param urls 저장할 URL 배열
   * @param batchId 배치 ID
   * @param batchTimestamp 배치 타임스탬프
   * @returns 생성된 요청 ID 배열
   */
  addSaveRequestsFromBroadcast: (
    urls: string[],
    batchId: string,
    batchTimestamp: number,
  ) => string[];

  /**
   * 외부(다른 탭)에서 저장 완료 브로드캐스트 받음
   * @param urls 저장된 URL 배열
   * @param batchId 배치 ID
   * @param success 성공 여부
   * @param error 에러 메시지 (선택적)
   */
  updateSaveStatusByUrls: (
    urls: string[],
    batchId: string,
    success: boolean,
    error?: string,
  ) => void;
}

/**
 * Save Status Store (Zustand)
 * - 저장 요청 진행 상황 관리
 * - 실시간 상태 업데이트
 * - UI 시각화를 위한 전역 상태
 */
export const useSaveStatusStore = create<SaveStatusStore>((set, get) => {
  // Store 생성 시 자동 초기화 (IIFE)
  void (async () => {
    try {
      const savedRequests = await storage.loadSaveStatusRequests();
      const requestsMap = new Map(savedRequests.map((req) => [req.id, req]));
      set({ saveRequests: requestsMap });
    } catch (error) {
      console.error('[SaveStatusStore] Failed to auto-initialize:', error);
    }
  })();

  // Storage 변경 감지 (자동 등록)
  storage.watchStorageChanges((key, newValue) => {
    if (key === storage.STORAGE_KEYS.SAVE_STATUS_REQUESTS) {
      const requestsMap = new Map((newValue as SaveRequest[]).map((req) => [req.id, req]));
      set({ saveRequests: requestsMap });
    }
  });

  return {
    saveRequests: new Map(),

    /**
     * Store 초기화 (레거시 호환용 - 실제로는 자동 초기화됨)
     */
    initialize: async () => {
      // 이미 자동 초기화되므로 아무것도 안 함
    },

    /**
     * Storage 동기화 (레거시 호환용)
     */
    syncFromStorage: (requests: SaveRequest[]) => {
      const requestsMap = new Map(requests.map((req) => [req.id, req]));
      set({ saveRequests: requestsMap });
    },

    addSaveRequest: (url: string, batchId: string, batchTimestamp: number) => {
      const id = `save-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
      const request: SaveRequest = {
        id,
        url,
        status: 'saving',
        startTime: Date.now(),
        batchId,
        batchTimestamp,
      };

      set((state) => {
        const newRequests = new Map(state.saveRequests);
        newRequests.set(id, request);

        // chrome.storage에 저장
        const requestsArray = Array.from(newRequests.values());
        storage
          .saveSaveStatusRequests(requestsArray)
          .catch((err) => console.error('[SaveStatusStore] Failed to save to storage:', err));

        return { saveRequests: newRequests };
      });

      return id;
    },

    updateSaveStatus: (id: string, status: SaveRequestStatus, error?: string) => {
      set((state) => {
        const request = state.saveRequests.get(id);
        if (!request) return state;

        const updatedRequest: SaveRequest = {
          ...request,
          status,
          completedTime: status === 'success' || status === 'error' ? Date.now() : undefined,
          error: error || request.error,
        };

        const newRequests = new Map(state.saveRequests);
        newRequests.set(id, updatedRequest);

        // chrome.storage에 저장
        const requestsArray = Array.from(newRequests.values());
        storage
          .saveSaveStatusRequests(requestsArray)
          .catch((err) => console.error('[SaveStatusStore] Failed to save to storage:', err));

        return { saveRequests: newRequests };
      });
    },

    removeSaveRequest: (id: string) => {
      set((state) => {
        const newRequests = new Map(state.saveRequests);
        newRequests.delete(id);

        // chrome.storage에 저장
        const requestsArray = Array.from(newRequests.values());
        storage
          .saveSaveStatusRequests(requestsArray)
          .catch((err) => console.error('[SaveStatusStore] Failed to save to storage:', err));

        return { saveRequests: newRequests };
      });
    },

    clearCompletedRequests: () => {
      set((state) => {
        const newRequests = new Map(state.saveRequests);
        for (const [id, request] of newRequests.entries()) {
          if (request.status === 'success' || request.status === 'error') {
            newRequests.delete(id);
          }
        }

        // chrome.storage에 저장
        const requestsArray = Array.from(newRequests.values());
        storage
          .saveSaveStatusRequests(requestsArray)
          .catch((err) => console.error('[SaveStatusStore] Failed to save to storage:', err));

        return { saveRequests: newRequests };
      });
    },

    getSavingCount: () => {
      const requests = Array.from(get().saveRequests.values());
      return requests.filter((r) => r.status === 'saving').length;
    },

    getRequestList: () => {
      return Array.from(get().saveRequests.values()).sort((a, b) => b.startTime - a.startTime);
    },

    addSaveRequestsFromBroadcast: (urls: string[], batchId: string, batchTimestamp: number) => {
      const requestIds: string[] = [];

      set((state) => {
        const newRequests = new Map(state.saveRequests);

        urls.forEach((url) => {
          // 고유 ID 생성 (batchTimestamp + URL 해시)
          const urlHash = url.slice(8, 20).replace(/[^a-zA-Z0-9]/g, '');
          const id = `save-${batchTimestamp}-${urlHash}`;

          // 중복 방지 (이미 있으면 스킵)
          if (!newRequests.has(id)) {
            const request: SaveRequest = {
              id,
              url,
              status: 'saving',
              startTime: Date.now(),
              batchId,
              batchTimestamp,
            };
            newRequests.set(id, request);
            requestIds.push(id);
          }
        });

        // chrome.storage에 저장
        const requestsArray = Array.from(newRequests.values());
        storage
          .saveSaveStatusRequests(requestsArray)
          .catch((err) => console.error('[SaveStatusStore] Failed to save to storage:', err));

        return { saveRequests: newRequests };
      });

      return requestIds;
    },

    updateSaveStatusByUrls: (urls: string[], batchId: string, success: boolean, error?: string) => {
      set((state) => {
        const newRequests = new Map(state.saveRequests);
        const status: SaveRequestStatus = success ? 'success' : 'error';

        // batchId가 일치하고 URL이 포함된 모든 요청 업데이트
        for (const [id, request] of newRequests.entries()) {
          if (request.batchId === batchId && urls.includes(request.url)) {
            newRequests.set(id, {
              ...request,
              status,
              completedTime: Date.now(),
              error,
            });
          }
        }

        // chrome.storage에 저장
        const requestsArray = Array.from(newRequests.values());
        storage
          .saveSaveStatusRequests(requestsArray)
          .catch((err) => console.error('[SaveStatusStore] Failed to save to storage:', err));

        return { saveRequests: newRequests };
      });
    },
  };
});
