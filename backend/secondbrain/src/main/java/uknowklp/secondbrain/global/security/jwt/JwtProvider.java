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
import uknowklp.secondbrain.api.user.service.UserService;
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
	private final UserService userService;
	private SecretKey secretKey;

	public JwtProvider(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.expire-time.access}") Duration accessExpireTime,
		@Value("${jwt.expire-time.refresh}") Duration refreshExpireTime,
		UserService userService
	) {
		this.secret = secret;
		this.accessExpireTime = accessExpireTime.toMillis();
		this.refreshExpireTime = refreshExpireTime.toMillis();
		this.userService = userService;
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
	 * Access Token 생성
	 *
	 * @param user 사용자 정보
	 * @return 생성된 access token
	 */
	public String createAccessToken(User user) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + accessExpireTime);
		String tokenId = UUID.randomUUID().toString();

		return Jwts.builder()
			.subject(user.getEmail())
			.claim("userId", user.getId())
			.claim("role", "ROLE_USER")
			.claim("tokenType", "ACCESS")
			.claim("tokenId", tokenId)
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
	}

	/**
	 * Refresh Token 생성
	 *
	 * @param user 사용자 정보
	 * @return 생성된 refresh token
	 */
	public String createRefreshToken(User user) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + refreshExpireTime);
		String tokenId = UUID.randomUUID().toString();

		return Jwts.builder()
			.subject(user.getEmail())
			.claim("userId", user.getId())
			.claim("tokenType", "REFRESH")
			.claim("tokenId", tokenId)
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
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

	public Authentication getAuthentication(String token) {
		if (validateToken(token)) {
			Claims claims = getClaims(token);
			String email = claims.getSubject();
			String role = claims.get("role", String.class);
			Optional<User> userOpt = userService.findByEmail(email);

			if (userOpt.isPresent()) {

				User user = userOpt.get();
				UserDetails userDetails = new CustomUserDetails(user);
				Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(role));
				UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, "",
					authorities);
				log.info("Authenticated user: {}", email);
				return auth;
			}
		}
		return null;
	}

	/**
	 * 토큰에서 tokenId를 추출
	 * <p>
	 * JWT 검증을 수행하고 tokenId claim을 반환합니다.
	 * </p>
	 *
	 * @param token JWT 토큰 (유효성 검증 수행됨)
	 * @return tokenId
	 * @throws io.jsonwebtoken.JwtException 토큰이 유효하지 않은 경우
	 */
	public String getTokenId(String token) {
		Claims claims = getClaims(token);
		return claims.get("tokenId", String.class);
	}

	/**
	 * 토큰이 유효한 경우에만 tokenId 반환
	 *
	 * @param token JWT 토큰
	 * @return 유효한 경우 tokenId, 무효한 경우 empty Optional
	 */
	public Optional<String> getTokenIdIfValid(String token) {
		return getClaimsIfValid(token)
			.map(claims -> claims.get("tokenId", String.class));
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
	 * 토큰 타입 확인 (ACCESS 또는 REFRESH)
	 * <p>
	 * JWT 검증을 수행하고 tokenType claim을 반환합니다.
	 * </p>
	 *
	 * @param token JWT 토큰 (유효성 검증 수행됨)
	 * @return 토큰 타입 ("ACCESS" 또는 "REFRESH")
	 * @throws io.jsonwebtoken.JwtException 토큰이 유효하지 않은 경우
	 */
	public String getTokenType(String token) {
		Claims claims = getClaims(token);
		return claims.get("tokenType", String.class);
	}

	/**
	 * 토큰이 유효한 경우에만 tokenType 반환
	 *
	 * @param token JWT 토큰
	 * @return 유효한 경우 tokenType, 무효한 경우 empty Optional
	 */
	public Optional<String> getTokenTypeIfValid(String token) {
		return getClaimsIfValid(token)
			.map(claims -> claims.get("tokenType", String.class));
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