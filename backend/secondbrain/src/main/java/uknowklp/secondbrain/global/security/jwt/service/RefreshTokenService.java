package uknowklp.secondbrain.global.security.jwt.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Refresh Token 관리 서비스 (단순화 버전)
 * Redis를 사용하여 refresh token을 저장하고 검증합니다.
 *
 * 개선사항:
 * - 불필요한 재사용 감지 메커니즘 제거
 * - 단순한 Token Rotation 유지
 * - Redis 접근 최소화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RedisTemplate<String, Object> redisTemplate;

	// Redis key pattern
	private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

	/**
	 * Refresh token을 Redis에 저장
	 *
	 * @param userId       사용자 ID
	 * @param refreshToken Refresh token 문자열
	 * @param ttlSeconds   만료 시간 (초 단위)
	 * @throws RuntimeException Redis 연결 실패 시 (caller에서 처리 필요)
	 */
	public void storeRefreshToken(Long userId, String refreshToken, long ttlSeconds) {
		try {
			String key = REFRESH_TOKEN_PREFIX + userId;
			redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(ttlSeconds));
			log.debug("Refresh token stored. UserId: {}, TTL: {}s", userId, ttlSeconds);
		} catch (Exception e) {
			log.error("Redis connection error during refresh token storage. UserId: {}", userId, e);
			throw e; // Caller에서 적절히 처리하도록 예외 전파
		}
	}

	/**
	 * Refresh token 검증 (단순 버전)
	 *
	 * @param userId       사용자 ID
	 * @param refreshToken 검증할 토큰
	 * @return 유효 여부 (Redis 연결 실패 시 false 반환)
	 */
	public boolean validateRefreshToken(Long userId, String refreshToken) {
		try {
			String key = REFRESH_TOKEN_PREFIX + userId;
			String storedToken = (String) redisTemplate.opsForValue().get(key);

			boolean isValid = refreshToken != null && refreshToken.equals(storedToken);
			log.debug("Refresh token validation. UserId: {}, Valid: {}", userId, isValid);
			return isValid;
		} catch (Exception e) {
			log.error("Redis connection error during refresh token validation. UserId: {}", userId, e);
			return false; // Redis 장애 시 검증 실패로 처리
		}
	}

	/**
	 * Token Rotation 수행 (단순화 버전)
	 * 기존 토큰을 새 토큰으로 교체
	 *
	 * @param userId          사용자 ID
	 * @param newRefreshToken 새 토큰
	 * @param ttlSeconds      만료 시간
	 */
	public void rotateRefreshToken(Long userId, String newRefreshToken, long ttlSeconds) {
		// 단순히 새 토큰으로 덮어쓰기 (기존 토큰은 자동으로 무효화됨)
		storeRefreshToken(userId, newRefreshToken, ttlSeconds);
		log.info("Token rotation completed. UserId: {}", userId);
	}

	/**
	 * Refresh token 무효화 (삭제)
	 *
	 * @param userId 사용자 ID
	 */
	public void revokeRefreshToken(Long userId) {
		try {
			String key = REFRESH_TOKEN_PREFIX + userId;
			Boolean deleted = redisTemplate.delete(key);
			log.info("Refresh token revoked. UserId: {}, Deleted: {}", userId, deleted);
		} catch (Exception e) {
			log.error("Redis connection error during refresh token revocation. UserId: {}", userId, e);
			// 로그아웃은 best-effort로 처리 (실패해도 예외 던지지 않음)
		}
	}
}