import { useState, useSyncExternalStore } from 'react';
import browser from 'webextension-polyfill';
import type { UserInfo } from '@/types/auth';

/**
 * Chrome 확장프로그램 인증 상태 관리 훅
 * - useSyncExternalStore로 Background Service Worker 구독
 * - useEffect 없이 외부 시스템과 동기화
 * - 메시지 리스너를 통한 실시간 인증 상태 업데이트
 *
 * React 공식 문서 권장: useSyncExternalStore for external system subscription
 * https://react.dev/learn/you-might-not-need-an-effect#subscribing-to-an-external-store
 */

export interface AuthState {
  loading: boolean;
  authenticated: boolean;
  user: UserInfo | null;
}

interface AuthResponse {
  authenticated: boolean;
  user?: UserInfo;
}

// 인증 상태를 저장할 외부 스토어
let authStateCache: AuthState = {
  loading: true,
  authenticated: false,
  user: null,
};

// 구독자 리스트
const listeners = new Set<() => void>();

/**
 * Chrome Extension Background Service Worker 구독 함수
 * - AUTH_CHANGED 메시지를 수신하면 모든 구독자에게 알림
 * - storage.onChanged로 인증 상태 변경 감지 (이중 안전장치)
 * - cleanup 함수로 리스너 제거
 */
function subscribe(callback: () => void): () => void {
  listeners.add(callback);

  // 1. Background의 AUTH_CHANGED 메시지 리스너 설정
  const handleMessage = (message: unknown) => {
    const msg = message as { type: string };
    if (msg.type === 'AUTH_CHANGED') {
      // 인증 상태가 변경되면 다시 확인
      void updateAuthState();
    }
  };

  browser.runtime.onMessage.addListener(handleMessage);

  // 2. Storage 변경 리스너 추가 (이중 안전장치)
  const handleStorageChange = (
    changes: Record<string, browser.Storage.StorageChange>,
    areaName: string,
  ) => {
    if (areaName === 'local' && changes['authenticated']) {
      // 인증 상태가 storage에서 직접 변경되면 업데이트
      void updateAuthState();
    }
  };

  browser.storage.onChanged.addListener(handleStorageChange);

  return () => {
    listeners.delete(callback);
    browser.runtime.onMessage.removeListener(handleMessage);
    browser.storage.onChanged.removeListener(handleStorageChange);
  };
}

/**
 * 현재 인증 상태를 동기적으로 가져오는 함수
 * - useSyncExternalStore가 요구하는 동기 함수
 */
function getSnapshot(): AuthState {
  return authStateCache;
}

/**
 * Background Service Worker에서 인증 상태 업데이트
 * - 이 함수가 호출되면 모든 구독자에게 알림
 */
async function updateAuthState(): Promise<void> {
  try {
    const rawResponse = await browser.runtime.sendMessage({
      type: 'CHECK_AUTH',
    });
    const response = rawResponse as AuthResponse;

    authStateCache = {
      loading: false,
      authenticated: response.authenticated,
      user: response.user ?? null,
    };

    // 모든 구독자에게 상태 변경 알림
    listeners.forEach((listener) => listener());
  } catch (error) {
    console.error('Failed to check auth status:', error);
    authStateCache = {
      loading: false,
      authenticated: false,
      user: null,
    };

    listeners.forEach((listener) => listener());
  }
}

export function useExtensionAuth() {
  // useSyncExternalStore로 Background Service Worker 구독
  const authState = useSyncExternalStore(subscribe, getSnapshot);

  // 초기화 상태 추적 (렌더 중 계산)
  const [initialized, setInitialized] = useState(false);

  // 초기 로드: 마운트 시 한 번만 실행 (이벤트 핸들러 패턴)
  if (!initialized && authState.loading) {
    setInitialized(true);
    void updateAuthState();
  }

  async function login(): Promise<void> {
    authStateCache = { ...authStateCache, loading: true };
    listeners.forEach((listener) => listener());

    try {
      const rawResponse = await browser.runtime.sendMessage({
        type: 'LOGIN',
      });
      const response = rawResponse as AuthResponse;

      authStateCache = {
        loading: false,
        authenticated: response.authenticated,
        user: response.user ?? null,
      };

      listeners.forEach((listener) => listener());
    } catch (error) {
      console.error('Login failed:', error);
      authStateCache = {
        loading: false,
        authenticated: false,
        user: null,
      };

      listeners.forEach((listener) => listener());
    }
  }

  async function logout(): Promise<void> {
    authStateCache = { ...authStateCache, loading: true };
    listeners.forEach((listener) => listener());

    try {
      await browser.runtime.sendMessage({ type: 'LOGOUT' });

      authStateCache = {
        loading: false,
        authenticated: false,
        user: null,
      };

      listeners.forEach((listener) => listener());
    } catch (error) {
      console.error('Logout failed:', error);
      authStateCache = { ...authStateCache, loading: false };

      listeners.forEach((listener) => listener());
    }
  }

  function refetch(): void {
    void updateAuthState();
  }

  return {
    ...authState,
    login,
    logout,
    refetch,
  };
}
