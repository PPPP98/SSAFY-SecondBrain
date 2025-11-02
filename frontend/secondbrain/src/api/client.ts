import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';

import type { BaseResponse } from '@/shared/types/api';
import { useAuthStore } from '@/stores/authStore';

/**
 * Axios 인스턴스 생성
 * - baseURL: 환경 변수에서 가져옴
 * - withCredentials: true (쿠키 전송 허용)
 * - timeout: 10초
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL as string,
  withCredentials: true,
  timeout: 10000,
});

/**
 * Request Interceptor
 * - Access Token을 자동으로 헤더에 추가
 */
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken;

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  },
);

/**
 * Response Interceptor
 * - BaseResponse 구조 처리
 * - 401 에러 시 Token Refresh 시도
 */
apiClient.interceptors.response.use(
  (response) => {
    // Axios 인터셉터는 response 객체를 반환해야 함
    // response.data를 변환하여 다시 response에 할당
    if (
      response.data &&
      typeof response.data === 'object' &&
      'success' in response.data &&
      'data' in response.data
    ) {
      // BaseResponse 구조인 경우 그대로 유지
      return response;
    }

    // GET /api/users/me는 BaseResponse 없이 직접 반환
    return response;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    // /api/auth/refresh 자체의 401은 무시 (무한 루프 방지)
    if (originalRequest.url?.includes('/api/auth/refresh')) {
      return Promise.reject(error);
    }

    // 401 에러 시 Token Refresh 시도
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // Refresh Token API 호출
        const response =
          await apiClient.post<
            BaseResponse<{ accessToken: string; tokenType: string; expiresIn: number }>
          >('/api/auth/refresh');

        // response.data가 BaseResponse
        const baseResponse = response.data;

        if (baseResponse.success && baseResponse.data) {
          // 새 Access Token 저장
          const { accessToken } = baseResponse.data;
          useAuthStore.getState().setAccessToken(accessToken);

          // 원래 요청 재시도
          return apiClient.request(originalRequest);
        }
      } catch {
        // Refresh 실패 시 로그아웃 처리
        useAuthStore.getState().clearAuth();
        window.location.href = '/';
        return Promise.reject(new Error('Token refresh failed'));
      }
    }

    return Promise.reject(error);
  },
);

export default apiClient;
