/**
 * Extension 환경 변수
 */
const getEnv = (key: string, fallback: string): string => {
  const value = import.meta.env[key] as string | undefined;
  return value ?? fallback;
};

export const env = {
  apiBaseUrl: getEnv('VITE_API_BASE_URL', 'https://api.brainsecond.site'),
  oauth2LoginUrl: getEnv(
    'VITE_OAUTH2_LOGIN_URL',
    'https://api.brainsecond.site/oauth2/authorization/google',
  ),
} as const;
