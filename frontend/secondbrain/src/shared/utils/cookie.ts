/**
 * 쿠키 유틸리티 함수
 */

/**
 * 특정 쿠키가 존재하는지 확인
 * @param name - 쿠키 이름
 * @returns 쿠키 존재 여부
 */
export function hasCookie(name: string): boolean {
  return document.cookie.split(';').some((cookie) => cookie.trim().startsWith(`${name}=`));
}

/**
 * Refresh Token 쿠키 존재 여부 확인
 * @returns Refresh Token 쿠키 존재 여부
 */
export function hasRefreshToken(): boolean {
  // 백엔드에서 설정하는 Refresh Token 쿠키 이름
  return hasCookie('refreshToken');
}
