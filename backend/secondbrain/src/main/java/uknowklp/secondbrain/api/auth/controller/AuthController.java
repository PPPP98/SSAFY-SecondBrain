package uknowklp.secondbrain.api.auth.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.api.user.service.UserService;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponse;
import uknowklp.secondbrain.global.response.BaseResponseStatus;
import uknowklp.secondbrain.global.security.jwt.JwtProvider;
import uknowklp.secondbrain.global.security.jwt.dto.CustomUserDetails;
import uknowklp.secondbrain.global.security.jwt.dto.ExchangeCodeRequest;
import uknowklp.secondbrain.global.security.jwt.dto.TokenResponse;
import uknowklp.secondbrain.global.security.jwt.service.RefreshTokenService;
import uknowklp.secondbrain.global.security.jwt.service.TokenBlacklistService;

/**
 * 인증 관련 REST API 컨트롤러
 * - Authorization code exchange (OAuth 콜백 후 토큰 교환)
 * - Token refresh (access token 갱신)
 * - Logout (token 무효화)
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final JwtProvider jwtProvider;
	private final RefreshTokenService refreshTokenService;
	private final TokenBlacklistService blacklistService;
	private final UserService userService;
	private final RedisTemplate<String, Object> redisTemplate;

	private static final String AUTH_CODE_PREFIX = "auth_code:";

	/**
	 * OAuth2 로그인 후 임시 인증 코드를 Access Token으로 교환
	 * Authorization Code Pattern (RFC 6749)
	 *
	 * @param request 인증 코드 요청 (code 포함)
	 * @return Access token (Response Body로 안전하게 전달)
	 */
	@PostMapping("/exchange")
	public ResponseEntity<BaseResponse<TokenResponse>> exchangeCodeForToken(
		@Valid @RequestBody ExchangeCodeRequest request) {

		String code = request.getCode();
		String authCodeKey = AUTH_CODE_PREFIX + code;

		// 1. Redis에서 인증 코드로 userId 조회
		String userIdStr = (String) redisTemplate.opsForValue().get(authCodeKey);

		if (userIdStr == null) {
			log.warn("Invalid or expired authorization code: {}", code);
			throw new BaseException(BaseResponseStatus.INVALID_AUTH_CODE);
		}

		// 2. 인증 코드 즉시 삭제 (일회용 보장)
		redisTemplate.delete(authCodeKey);
		log.info("Authorization code consumed and deleted: {}", code);

		// 3. 사용자 조회
		Long userId = Long.parseLong(userIdStr);
		User user = userService.findById(userId)
			.orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

		// 4. Access Token 생성
		String accessToken = jwtProvider.createAccessToken(user);
		long accessExpireTime = jwtProvider.getAccessExpireTime();

		log.info("Access token generated for user: {}, userId: {}", user.getEmail(), userId);

		// 5. Response Body로 Access Token 반환 (보안 강화)
		TokenResponse tokenResponse = TokenResponse.of(accessToken, accessExpireTime);
		return ResponseEntity.ok(new BaseResponse<>(tokenResponse));
	}

	/**
	 * Refresh Token으로 새로운 Access Token 발급
	 * Token Rotation: 새로운 refresh token도 함께 발급하여 보안 강화
	 *
	 * @param refreshToken Refresh token (쿠키에서 자동 추출)
	 * @param response     HTTP 응답 (새 refresh token 쿠키 설정용)
	 * @return 새로운 access token
	 */
	@PostMapping("/refresh")
	public ResponseEntity<BaseResponse<TokenResponse>> refresh(
		@CookieValue(name = "refreshToken", required = false) String refreshToken,
		HttpServletResponse response) {

		// 1. Refresh token 존재 여부 확인
		if (refreshToken == null || refreshToken.isEmpty()) {
			throw new BaseException(BaseResponseStatus.REFRESH_TOKEN_NOT_FOUND);
		}

		// 2. Refresh token JWT 검증
		if (!jwtProvider.validateToken(refreshToken)) {
			throw new BaseException(BaseResponseStatus.INVALID_REFRESH_TOKEN);
		}

		Claims claims = jwtProvider.getClaims(refreshToken);
		String userId = String.valueOf(claims.get("userId"));
		String tokenId = claims.get("tokenId", String.class);
		String tokenType = claims.get("tokenType", String.class);

		// 3. Token 타입 확인
		if (!"REFRESH".equals(tokenType)) {
			throw new BaseException(BaseResponseStatus.INVALID_REFRESH_TOKEN);
		}

		// 4. Redis에서 검증 (서버 측 검증)
		if (!refreshTokenService.validateRefreshToken(userId, tokenId)) {
			throw new BaseException(BaseResponseStatus.REFRESH_TOKEN_NOT_FOUND);
		}

		// 5. Blacklist 확인 (재사용 시도 감지)
		if (blacklistService.isBlacklisted(tokenId, "REFRESH")) {
			// 하이재킹 감지: 이미 사용된 토큰 재사용 시도
			log.error("Token hijacking detected! UserId: {}, TokenId: {}", userId, tokenId);
			refreshTokenService.revokeAllUserTokens(userId);
			throw new BaseException(BaseResponseStatus.TOKEN_HIJACKING_DETECTED);
		}

		// 6. 사용자 조회
		User user = userService.findById(Long.parseLong(userId))
			.orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

		// 7. 새로운 토큰 생성 (Token Rotation)
		String newAccessToken = jwtProvider.createAccessToken(user);
		String newRefreshToken = jwtProvider.createRefreshToken(user);
		String newRefreshTokenId = jwtProvider.getTokenId(newRefreshToken);

		// 8. 이전 refresh token 무효화 (Rotation)
		long refreshExpireSeconds = jwtProvider.getRefreshExpireTime() / 1000;
		blacklistService.addToBlacklist(tokenId, "REFRESH", refreshExpireSeconds);
		refreshTokenService.revokeRefreshToken(userId, tokenId);

		// 9. 새 refresh token 저장
		refreshTokenService.storeRefreshToken(
			userId,
			newRefreshToken,
			newRefreshTokenId,
			refreshExpireSeconds
		);

		// 10. 새 refresh token을 HttpOnly 쿠키로 설정
		Cookie refreshCookie = new Cookie("refreshToken", newRefreshToken);
		refreshCookie.setHttpOnly(true);
		refreshCookie.setSecure(true);  // HTTPS only (프로덕션)
		refreshCookie.setPath("/");
		refreshCookie.setMaxAge((int)refreshExpireSeconds);
		response.addCookie(refreshCookie);

		log.info("Token refreshed successfully. UserId: {}", userId);

		// 11. 새 access token 응답
		TokenResponse tokenResponse = TokenResponse.of(newAccessToken, jwtProvider.getAccessExpireTime());
		return ResponseEntity.ok(new BaseResponse<>(tokenResponse));
	}

	/**
	 * 로그아웃 처리
	 * - Access token을 blacklist에 추가 (남은 수명 동안)
	 * - Refresh token을 무효화
	 * - Refresh token 쿠키 삭제
	 *
	 * @param userDetails  인증된 사용자 정보
	 * @param refreshToken Refresh token (쿠키에서 자동 추출)
	 * @param authHeader   Authorization 헤더 (access token)
	 * @param response     HTTP 응답 (쿠키 삭제용)
	 * @return 성공 응답
	 */
	@PostMapping("/logout")
	public ResponseEntity<BaseResponse<Void>> logout(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@CookieValue(name = "refreshToken", required = false) String refreshToken,
		@RequestHeader(value = "Authorization", required = false) String authHeader,
		HttpServletResponse response) {

		String userId = String.valueOf(userDetails.getUser().getId());

		// 1. Access token blacklist 추가
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String accessToken = authHeader.substring(7);
			if (jwtProvider.validateToken(accessToken)) {
				String accessTokenId = jwtProvider.getTokenId(accessToken);
				Claims claims = jwtProvider.getClaims(accessToken);
				long remainingTime = claims.getExpiration().getTime() - System.currentTimeMillis();

				if (remainingTime > 0) {
					blacklistService.addToBlacklist(accessTokenId, "ACCESS", remainingTime / 1000);
					log.info("Access token added to blacklist. UserId: {}, TokenId: {}", userId, accessTokenId);
				}
			}
		}

		// 2. Refresh token 무효화
		if (refreshToken != null && jwtProvider.validateToken(refreshToken)) {
			String refreshTokenId = jwtProvider.getTokenId(refreshToken);
			refreshTokenService.revokeRefreshToken(userId, refreshTokenId);
			log.info("Refresh token revoked. UserId: {}, TokenId: {}", userId, refreshTokenId);
		}

		// 3. Refresh token 쿠키 삭제
		Cookie refreshCookie = new Cookie("refreshToken", null);
		refreshCookie.setHttpOnly(true);
		refreshCookie.setSecure(true);
		refreshCookie.setPath("/");
		refreshCookie.setMaxAge(0);
		response.addCookie(refreshCookie);

		log.info("User logged out successfully. UserId: {}, Email: {}", userId, userDetails.getUsername());

		return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS));
	}
}
