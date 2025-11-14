import { useState, useEffect } from 'react';

type OverlayState = 'expanded' | 'collapsed' | 'hidden';

const STORAGE_KEY = 'secondbrain-overlay-state';

export interface OverlayStateHook {
  state: OverlayState;
  isExpanded: boolean;
  isCollapsed: boolean;
  isHidden: boolean;
  expand: () => void;
  collapse: () => void;
  hide: () => void;
  toggle: () => void;
}

export function useOverlayState(): OverlayStateHook {
  const [state, setState] = useState<OverlayState>(() => {
    // 항상 expanded로 시작 (localStorage 무시)
    return 'expanded';
  });

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, state);
  }, [state]);

  return {
    state,
    isExpanded: state === 'expanded',
    isCollapsed: state === 'collapsed',
    isHidden: state === 'hidden',
    expand: () => setState('expanded'),
    collapse: () => setState('collapsed'),
    hide: () => setState('hidden'),
    toggle: () => setState((s) => (s === 'expanded' ? 'collapsed' : 'expanded')),
  };
}
