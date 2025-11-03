import { create } from 'zustand';

import type { UserInfo } from '@/features/auth/types/auth';

/**
 * 인증 스토어 상태 인터페이스
 */
interface AuthStore {
  accessToken: string | null;
  user: UserInfo | null;
  isAuthenticated: boolean;
  setAccessToken: (token: string) => void;
  setUser: (user: UserInfo) => void;
  clearAuth: () => void;
}

/**
 * 인증 상태 관리 Zustand 스토어
 * - accessToken: 메모리에 저장 (XSS 방어)
 * - user: 현재 로그인된 사용자 정보
 * - isAuthenticated: 로그인 여부
 */
export const useAuthStore = create<AuthStore>()((set) => ({
  accessToken: null,
  user: null,
  isAuthenticated: false,

  setAccessToken: (token) => set({ accessToken: token, isAuthenticated: true }),

  setUser: (user) => set({ user }),

  clearAuth: () => set({ accessToken: null, user: null, isAuthenticated: false }),
}));
