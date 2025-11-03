package uknowklp.secondbrain.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.global.security.jwt.dto.CustomUserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtProvider {

	private final String secret;
	private final long accessExpireTime;
	private final long refreshExpireTime;
	private SecretKey secretKey;

	public JwtProvider(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.expire-time.access}") Duration accessExpireTime,
		@Value("${jwt.expire-time.refresh}") Duration refreshExpireTime
	) {
		this.secret = secret;
		this.accessExpireTime = accessExpireTime.toMillis();
		this.refreshExpireTime = refreshExpireTime.toMillis();
	}

	@PostConstruct
	private void init() {
		// JWT Secret 키 길이 검증 (HS256은 최소 256비트/32바이트 필요)
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalArgumentException(
				String.format(
					"JWT secret must be at least 256 bits (32 bytes). Current length: %d bytes. " +
						"Please use a longer secret key for security.",
					keyBytes.length
				)
			);
		}

		// SecretKey 객체 생성
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
		log.info("JWT SecretKey initialized successfully (length: {} bytes)", keyBytes.length);
	}

	/**
	 * JWT 토큰 생성 (공통 메서드)
	 *
	 * @param user 사용자 정보
	 * @param expireTime 만료 시간 (밀리초)
	 * @return 생성된 JWT 토큰
	 */
	private String createToken(User user, long expireTime) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expireTime);

		return Jwts.builder()
			.subject(user.getEmail())
			.claim("userId", user.getId())
			.claim("role", "ROLE_USER")
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
	}

	/**
	 * Access Token 생성
	 *
	 * @param user 사용자 정보
	 * @return 생성된 access token
	 */
	public String createAccessToken(User user) {
		return createToken(user, accessExpireTime);
	}

	/**
	 * Refresh Token 생성
	 *
	 * @param user 사용자 정보
	 * @return 생성된 refresh token
	 */
	public String createRefreshToken(User user) {
		return createToken(user, refreshExpireTime);
	}

	// 토큰 유효성 검증
	public boolean validateToken(String token) {
		try {
			Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
			return true;
		} catch (Exception e) {
			log.warn("Invalid JWT token: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * 토큰 검증 및 Claims 추출
	 * <p>
	 * JWT 서명 검증, 만료 시간 검사 등을 자동으로 수행합니다.
	 * 토큰이 유효하지 않으면 예외가 발생합니다.
	 * </p>
	 *
	 * @param token JWT 토큰
	 * @return Claims 객체
	 * @throws io.jsonwebtoken.JwtException 토큰이 유효하지 않은 경우
	 *         (서명 오류, 만료, 형식 오류 등)
	 */
	public Claims getClaims(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	/**
	 * 토큰이 유효한 경우에만 Claims 반환
	 * <p>
	 * JWT 검증을 수행하고 성공 시 Claims를 반환합니다.
	 * 검증 실패 시 예외를 로깅하고 empty Optional을 반환합니다.
	 * </p>
	 *
	 * @param token JWT 토큰
	 * @return 유효한 경우 Claims를 포함한 Optional, 무효한 경우 empty Optional
	 */
	public Optional<Claims> getClaimsIfValid(String token) {
		try {
			return Optional.of(getClaims(token));
		} catch (Exception e) {
			log.warn("Failed to parse JWT token: {}", e.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * JWT 토큰으로부터 Authentication 객체 생성
	 * <p>
	 * 성능 최적화: DB 조회 없이 JWT claims만으로 User 객체 생성
	 * 인증에는 userId, email, role만 필요하며 모두 JWT에 포함되어 있음
	 * 토큰 파싱 최적화: getClaimsIfValid()를 사용하여 한 번만 파싱
	 * </p>
	 * <p>
	 * ⚠️ 중요: 생성된 User 객체는 인증(Authentication) 전용입니다.
	 * - User 엔티티의 필수 필드(name, setAlarm)가 null입니다.
	 * - 비즈니스 로직에서 이 User 객체를 직접 사용하지 마세요.
	 * - 필요 시 UserService.findById()로 완전한 User 엔티티를 조회하세요.
	 * </p>
	 *
	 * @param token JWT 토큰
	 * @return Authentication 객체 또는 null
	 */
	public Authentication getAuthentication(String token) {
		return getClaimsIfValid(token)
			.map(claims -> {
				// JWT claims로부터 직접 User 객체 생성 (DB 조회 불필요)
				// ⚠️ 주의: 이 User 객체는 인증 전용이며 name, setAlarm 필드가 null입니다.
				User user = User.builder()
					.id(claims.get("userId", Long.class))
					.email(claims.getSubject())
					.build();

				UserDetails userDetails = new CustomUserDetails(user);
				String role = claims.get("role", String.class);
				Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(role));
				UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
					userDetails, "", authorities);

				log.debug("Authenticated user from JWT: {}", user.getEmail());
				return auth;
			})
			.orElse(null);
	}

	/**
	 * 토큰에서 userId를 추출
	 * <p>
	 * JWT 검증을 수행하고 userId claim을 반환합니다.
	 * </p>
	 *
	 * @param token JWT 토큰 (유효성 검증 수행됨)
	 * @return userId
	 * @throws io.jsonwebtoken.JwtException 토큰이 유효하지 않은 경우
	 */
	public Long getUserId(String token) {
		Claims claims = getClaims(token);
		return claims.get("userId", Long.class);
	}

	/**
	 * 토큰이 유효한 경우에만 userId 반환
	 *
	 * @param token JWT 토큰
	 * @return 유효한 경우 userId, 무효한 경우 empty Optional
	 */
	public Optional<Long> getUserIdIfValid(String token) {
		return getClaimsIfValid(token)
			.map(claims -> claims.get("userId", Long.class));
	}

	/**
	 * Access Token 만료 시간을 반환 (밀리초)
	 *
	 * @return access token 만료 시간
	 */
	public long getAccessExpireTime() {
		return accessExpireTime;
	}

	/**
	 * Refresh Token 만료 시간을 반환 (밀리초)
	 *
	 * @return refresh token 만료 시간
	 */
	public long getRefreshExpireTime() {
		return refreshExpireTime;
	}
}