/**
 * 환경 변수 타입 안전 관리
 *
 * 이 모듈은 Vite의 환경 변수에 대한 런타임 검증 레이어를 제공합니다.
 * - 타입 정의: src/vite-env.d.ts (Vite 공식 패턴)
 * - 런타임 검증: 이 파일 (환경 변수 누락 시 즉시 에러)
 * - 중앙 관리: 모든 환경 변수를 한 곳에서 관리
 *
 * 베스트 프랙티스:
 * - import.meta.env 직접 사용 시에도 타입 추론 가능 (vite-env.d.ts)
 * - env 객체 사용 시 런타임 검증 + 중앙 관리 (이 파일)
 */

/**
 * 환경 변수 안전하게 가져오기
 * - vite-env.d.ts의 ImportMetaEnv 타입 활용
 * - 환경 변수 누락 시 명확한 에러 메시지 제공
 */
const getEnvVar = <Key extends keyof ImportMetaEnv>(key: Key): ImportMetaEnv[Key] => {
  const value = import.meta.env[key];
  if (!value) {
    throw new Error(
      `Missing required environment variable: ${key}\n` +
        `Please check your .env file and ensure ${key} is defined.`,
    );
  }
  // ESLint: import.meta.env의 값은 안전하게 타입 단언 가능
  // eslint-disable-next-line @typescript-eslint/no-unsafe-return
  return value as ImportMetaEnv[Key];
};

/**
 * 타입 안전하고 검증된 환경 변수 객체
 * - vite-env.d.ts에서 타입 정의
 * - 이 파일에서 런타임 검증
 * - 중앙 집중식 관리로 유지보수성 향상
 *
 * @example
 * ```ts
 * import { env } from '@/config/env';
 *
 * const response = await fetch(`${env.apiBaseUrl}/users`);
 * ```
 */
export const env = {
  /** API 서버 기본 URL */
  apiBaseUrl: getEnvVar('VITE_API_BASE_URL'),
  /** OAuth2 Google 로그인 URL */
  oauth2LoginUrl: getEnvVar('VITE_OAUTH2_LOGIN_URL'),
} as const;

/**
 * 환경 변수 객체 타입 (읽기 전용)
 */
export type Env = typeof env;
