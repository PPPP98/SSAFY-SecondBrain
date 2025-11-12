/**
 * 환경 변수 타입 안전 관리
 * - 환경 변수 누락 시 명확한 에러 메시지 제공
 * - TypeScript 타입 검증 (vite-env.d.ts)
 */

const ENV_HINTS: Partial<Record<keyof ImportMetaEnv, string>> = {
  VITE_API_BASE_URL: 'http://localhost:8080',
  VITE_OAUTH2_LOGIN_URL: 'http://localhost:8080/oauth2/authorization/google',
};

const getEnvVar = <Key extends keyof ImportMetaEnv>(key: Key): ImportMetaEnv[Key] => {
  const value = import.meta.env[key];
  if (!value) {
    const hint = ENV_HINTS[key];
    throw new Error(
      `❌ 필수 환경 변수가 설정되지 않았습니다: ${key}\n\n` +
        (hint ? `📝 설정 예시:\n   ${hint}\n\n` : '') +
        `📂 파일 위치: extension/.env\n\n` +
        `💡 .env 파일을 확인하고 ${key} 변수를 설정해주세요.`,
    );
  }
  // eslint-disable-next-line @typescript-eslint/no-unsafe-return
  return value as ImportMetaEnv[Key];
};

export const env = {
  apiBaseUrl: getEnvVar('VITE_API_BASE_URL'),
  oauth2LoginUrl: getEnvVar('VITE_OAUTH2_LOGIN_URL'),
} as const;

export type Env = typeof env;
