package uknowklp.secondbrain.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

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

	public String createToken(User user) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + accessExpireTime);

		return Jwts.builder()
			.subject(user.getEmail())
			.claim("userId", user.getId())
			.claim("role", "ROLE_USER")
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

	// 토큰에서 사용자 정보를 추출하는 메서드
	public Claims getClaims(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload();
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
}