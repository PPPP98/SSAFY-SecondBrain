import { create } from 'zustand';

/**
 * Graph 렌더링 상태 관리 Store
 * - GPU 최적화를 위한 pause/resume 제어
 * - SidePeekOverlay 등 오버레이 컴포넌트에서 사용
 */
interface GraphState {
  /** Graph 렌더링 일시정지 여부 */
  isPaused: boolean;

  /** Graph 렌더링 일시정지 (GPU 절약) */
  pauseGraph: () => void;

  /** Graph 렌더링 재개 */
  resumeGraph: () => void;
}

export const useGraphStore = create<GraphState>((set) => ({
  isPaused: false,

  pauseGraph: () => set({ isPaused: true }),

  resumeGraph: () => set({ isPaused: false }),
}));
