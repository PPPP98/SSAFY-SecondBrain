import { useState, useEffect } from 'react';
import * as storage from '@/services/storageService';
import type { OverlayState } from '@/services/storageService';

export interface OverlayStateHook {
  state: OverlayState;
  isLoading: boolean;
  isExpanded: boolean;
  isCollapsed: boolean;
  isHidden: boolean;
  expand: () => Promise<void>;
  collapse: () => Promise<void>;
  hide: () => Promise<void>;
  toggle: () => Promise<void>;
}

/**
 * Overlay State Hook (chrome.storage 기반)
 * - chrome.storage.local로 상태 영구 저장
 * - 탭 간 상태 동기화
 * - 페이지 새로고침 후에도 유지
 * - Optimistic Update 패턴으로 즉각적인 UI 반응
 */
export function useOverlayState(): OverlayStateHook {
  const [state, setState] = useState<OverlayState>('expanded');
  const [isLoading, setIsLoading] = useState(true);

  // 초기화: chrome.storage에서 상태 로드
  useEffect(() => {
    void (async () => {
      try {
        const savedState = await storage.loadOverlayState();
        setState(savedState);
        console.log('[useOverlayState] Initialized with state:', savedState);
      } catch (error) {
        console.error('[useOverlayState] Failed to load state:', error);
        // 실패해도 기본값으로 계속 진행
      } finally {
        setIsLoading(false);
      }
    })();
  }, []);

  // storage.onChanged 리스너: 다른 탭의 변경 감지
  useEffect(() => {
    const unwatch = storage.watchStorageChanges((key, newValue) => {
      if (key === storage.STORAGE_KEYS.OVERLAY_STATE) {
        setState(newValue as OverlayState);
        console.log('[useOverlayState] Synced from storage:', newValue);
      }
    });

    // Cleanup: 컴포넌트 언마운트 시 리스너 제거
    return unwatch;
  }, []);

  /**
   * 상태 업데이트 함수 (Optimistic Update)
   * 1. UI 즉시 업데이트 (동기)
   * 2. Storage 저장 (비동기)
   */
  const updateState = async (newState: OverlayState) => {
    // 1단계: UI 즉시 업데이트
    setState(newState);

    // 2단계: Storage 저장 (비동기)
    try {
      await storage.saveOverlayState(newState);
    } catch (error) {
      console.error('[useOverlayState] Failed to save state:', error);
      // Storage 실패해도 UI 상태는 유지 (로컬 상태로 작동)
    }
  };

  return {
    state,
    isLoading,
    isExpanded: state === 'expanded',
    isCollapsed: state === 'collapsed',
    isHidden: state === 'hidden',
    expand: () => updateState('expanded'),
    collapse: () => updateState('collapsed'),
    hide: () => updateState('hidden'),
    toggle: () => updateState(state === 'expanded' ? 'collapsed' : 'expanded'),
  };
}
