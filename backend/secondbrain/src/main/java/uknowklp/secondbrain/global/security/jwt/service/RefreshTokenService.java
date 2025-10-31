package uknowklp.secondbrain.global.security.jwt.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Refresh Token 관리 서비스 (단순화 버전)
 * Redis를 사용하여 refresh token을 단순하게 저장하고 검증합니다.
 *
 * 개선사항:
 * - 복잡한 메타데이터 제거
 * - Token rotation 제거
 * - 단순 key-value 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RedisTemplate<String, Object> redisTemplate;

	// Redis key pattern: refresh_token:{userId}:{tokenId}
	private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

	/**
	 * Refresh token을 Redis에 저장 (단순화)
	 *
	 * @param userId       사용자 ID
	 * @param refreshToken Refresh token 문자열
	 * @param tokenId      Token의 고유 ID
	 * @param ttlSeconds   만료 시간 (초 단위)
	 */
	public void storeRefreshToken(String userId, String refreshToken, String tokenId, long ttlSeconds) {
		String key = REFRESH_TOKEN_PREFIX + userId + ":" + tokenId;

		// 단순하게 토큰 존재 여부만 저장 (값은 "valid" 문자열)
		redisTemplate.opsForValue().set(key, "valid", Duration.ofSeconds(ttlSeconds));

		log.debug("Refresh token stored. UserId: {}, TokenId: {}, TTL: {}s", userId, tokenId, ttlSeconds);
	}

	/**
	 * Refresh token이 Redis에 존재하는지 검증
	 *
	 * @param userId  사용자 ID
	 * @param tokenId Token의 고유 ID
	 * @return 존재 여부
	 */
	public boolean validateRefreshToken(String userId, String tokenId) {
		String key = REFRESH_TOKEN_PREFIX + userId + ":" + tokenId;
		Boolean exists = redisTemplate.hasKey(key);

		log.debug("Refresh token validation. UserId: {}, TokenId: {}, Exists: {}", userId, tokenId, exists);
		return Boolean.TRUE.equals(exists);
	}

	/**
	 * 특정 refresh token을 무효화 (삭제)
	 *
	 * @param userId  사용자 ID
	 * @param tokenId Token의 고유 ID
	 */
	public void revokeRefreshToken(String userId, String tokenId) {
		String key = REFRESH_TOKEN_PREFIX + userId + ":" + tokenId;
		Boolean deleted = redisTemplate.delete(key);

		log.info("Refresh token revoked. UserId: {}, TokenId: {}, Deleted: {}", userId, tokenId, deleted);
	}
}