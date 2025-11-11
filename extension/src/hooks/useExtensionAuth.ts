import { useEffect, useState } from 'react';
import browser from 'webextension-polyfill';

/**
 * Chrome 확장프로그램 인증 상태 관리 훅
 * - Background Service Worker와 통신하여 인증 상태 확인
 * - JWT 쿠키 기반 자동 로그인
 * - 로그인/로그아웃 기능 제공
 */

export interface User {
  id: string;
  name: string;
  email: string;
}

export interface AuthState {
  loading: boolean;
  authenticated: boolean;
  user: User | null;
}

interface AuthResponse {
  authenticated: boolean;
  user?: {
    id: string;
    name: string;
    email: string;
  };
}

export function useExtensionAuth() {
  const [authState, setAuthState] = useState<AuthState>({
    loading: true,
    authenticated: false,
    user: null,
  });

  // 초기 인증 상태 확인
  useEffect(() => {
    void checkAuthStatus();

    // Background에서 AUTH_CHANGED 메시지 수신 시 인증 상태 재확인
    const handleMessage = (message: unknown) => {
      const msg = message as { type: string };
      if (msg.type === 'AUTH_CHANGED') {
        void checkAuthStatus();
      }
    };

    browser.runtime.onMessage.addListener(handleMessage);

    return () => {
      browser.runtime.onMessage.removeListener(handleMessage);
    };
  }, []);

  async function checkAuthStatus(): Promise<void> {
    try {
      const rawResponse = await browser.runtime.sendMessage({
        type: 'CHECK_AUTH',
      });
      const response = rawResponse as AuthResponse;

      setAuthState({
        loading: false,
        authenticated: response.authenticated,
        user: response.user ?? null,
      });
    } catch (error) {
      console.error('Failed to check auth status:', error);
      setAuthState({
        loading: false,
        authenticated: false,
        user: null,
      });
    }
  }

  async function login(): Promise<void> {
    setAuthState((prev) => ({ ...prev, loading: true }));

    try {
      const rawResponse = await browser.runtime.sendMessage({
        type: 'LOGIN',
      });
      const response = rawResponse as AuthResponse;

      setAuthState({
        loading: false,
        authenticated: response.authenticated,
        user: response.user ?? null,
      });
    } catch (error) {
      console.error('Login failed:', error);
      setAuthState({
        loading: false,
        authenticated: false,
        user: null,
      });
    }
  }

  async function logout(): Promise<void> {
    setAuthState((prev) => ({ ...prev, loading: true }));

    try {
      await browser.runtime.sendMessage({ type: 'LOGOUT' });

      setAuthState({
        loading: false,
        authenticated: false,
        user: null,
      });
    } catch (error) {
      console.error('Logout failed:', error);
      setAuthState((prev) => ({ ...prev, loading: false }));
    }
  }

  return {
    ...authState,
    login,
    logout,
    refetch: checkAuthStatus,
  };
}
