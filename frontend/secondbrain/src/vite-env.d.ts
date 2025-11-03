/// <reference types="vite/client" />

/**
 * Vite 환경 변수 타입 정의
 * - import.meta.env에 대한 TypeScript IntelliSense 제공
 * - 모든 VITE_ 접두사 환경 변수를 여기에 정의
 *
 * ⚠️ 중요: 이 파일에는 import/export 문을 포함하지 마세요.
 * import/export가 있으면 TypeScript가 파일을 모듈로 처리하여
 * 타입 augmentation이 작동하지 않습니다.
 */

interface ImportMetaEnv {
  /** API 서버 기본 URL */
  readonly VITE_API_BASE_URL: string;
  /** OAuth2 Google 로그인 URL */
  readonly VITE_OAUTH2_LOGIN_URL: string;

  // Vite 기본 환경 변수
  readonly MODE: string;
  readonly BASE_URL: string;
  readonly PROD: boolean;
  readonly DEV: boolean;
  readonly SSR: boolean;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
