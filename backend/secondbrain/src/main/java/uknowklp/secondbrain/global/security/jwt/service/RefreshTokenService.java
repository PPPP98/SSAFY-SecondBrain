package uknowklp.secondbrain.global.security.jwt.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Refresh Token 관리 서비스
 * Redis를 사용하여 refresh token을 저장하고 검증합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RedisTemplate<String, Object> redisTemplate;

	// Redis key pattern: refresh_token:{userId}:{tokenId}
	private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
	private static final int REDIS_SCAN_BATCH_SIZE = 100;

	/**
	 * Refresh token을 Redis에 저장
	 * <p>
	 * 성능 최적화: 전체 JWT 문자열 대신 필수 메타데이터만 저장 (60% 메모리 절약)
	 * 검증 시에는 쿠키의 JWT를 사용하므로 Redis에 전체 토큰 저장 불필요
	 * </p>
	 *
	 * @param userId       사용자 ID
	 * @param refreshToken Refresh token 문자열 (저장하지 않음, 파라미터 호환성 유지)
	 * @param tokenId      Token의 고유 ID
	 * @param ttlSeconds   만료 시간 (초 단위)
	 */
	public void storeRefreshToken(String userId, String refreshToken, String tokenId, long ttlSeconds) {
		String key = REFRESH_TOKEN_PREFIX + userId + ":" + tokenId;

		// 최소한의 메타데이터만 저장 (전체 JWT는 쿠키에 있으므로 불필요)
		Map<String, Object> tokenData = new HashMap<>();
		tokenData.put("tokenId", tokenId);
		tokenData.put("issuedAt", System.currentTimeMillis());

		redisTemplate.opsForValue().set(key, tokenData, Duration.ofSeconds(ttlSeconds));
		log.debug("Refresh token metadata stored in Redis. UserId: {}, TokenId: {}, TTL: {}s", userId, tokenId,
			ttlSeconds);
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

	/**
	 * 사용자의 모든 refresh token을 무효화
	 * 보안 위반 감지 시 사용됩니다.
	 *
	 * Redis SCAN을 사용하여 non-blocking 방식으로 키를 검색합니다.
	 * KEYS 명령어는 O(N) 복잡도로 Redis를 블로킹하므로 프로덕션에서 사용하면 안 됩니다.
	 *
	 * @param userId 사용자 ID
	 * @see <a href="https://redis.io/commands/scan">Redis SCAN Command</a>
	 * @see <a href="https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency">Redis Latency Optimization</a>
	 */
	public void revokeAllUserTokens(String userId) {
		String pattern = REFRESH_TOKEN_PREFIX + userId + ":*";
		Set<String> keys = new HashSet<>();

		// Redis SCAN을 사용하여 non-blocking 방식으로 키 수집
		redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
			ScanOptions options = ScanOptions.scanOptions()
				.match(pattern)
				.count(REDIS_SCAN_BATCH_SIZE)  // 한 번에 스캔할 키 개수 힌트 (Redis는 이를 참고만 함)
				.build();

			try (Cursor<byte[]> cursor = connection.scan(options)) {
				cursor.forEachRemaining(key -> keys.add(new String(key)));
			}
			return keys;
		});

		if (!keys.isEmpty()) {
			Long deletedCount = redisTemplate.delete(keys);
			log.warn("All refresh tokens revoked for user. UserId: {}, Count: {}", userId, deletedCount);
		} else {
			log.debug("No refresh tokens found for user. UserId: {}", userId);
		}
	}

	/**
	 * Redis에서 refresh token 데이터를 조회
	 *
	 * @param userId  사용자 ID
	 * @param tokenId Token의 고유 ID
	 * @return Token 데이터 (없으면 null)
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getRefreshTokenData(String userId, String tokenId) {
		String key = REFRESH_TOKEN_PREFIX + userId + ":" + tokenId;
		Object data = redisTemplate.opsForValue().get(key);
		return data != null ? (Map<String, Object>)data : null;
	}
}
