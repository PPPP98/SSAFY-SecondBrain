import { create } from 'zustand';
import type { SaveRequest, SaveRequestStatus } from '@/types/save';

interface SaveStatusStore {
  /** 진행 중인 저장 요청 맵 (id → SaveRequest) */
  saveRequests: Map<string, SaveRequest>;

  /**
   * 새 저장 요청 추가
   * @param url 저장할 URL
   * @returns 생성된 요청 ID
   */
  addSaveRequest: (url: string) => string;

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
}

/**
 * Save Status Store (Zustand)
 * - 저장 요청 진행 상황 관리
 * - 실시간 상태 업데이트
 * - UI 시각화를 위한 전역 상태
 */
export const useSaveStatusStore = create<SaveStatusStore>((set, get) => ({
  saveRequests: new Map(),

  addSaveRequest: (url: string) => {
    const id = `save-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
    const request: SaveRequest = {
      id,
      url,
      status: 'saving',
      startTime: Date.now(),
    };

    set((state) => {
      const newRequests = new Map(state.saveRequests);
      newRequests.set(id, request);
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
      return { saveRequests: newRequests };
    });
  },

  removeSaveRequest: (id: string) => {
    set((state) => {
      const newRequests = new Map(state.saveRequests);
      newRequests.delete(id);
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
}));
