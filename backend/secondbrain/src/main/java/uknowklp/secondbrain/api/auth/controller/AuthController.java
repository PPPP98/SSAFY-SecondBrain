package uknowklp.secondbrain.api.auth.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.auth.dto.GoogleTokenRequest;
import uknowklp.secondbrain.api.auth.dto.GoogleTokenResponse;
import uknowklp.secondbrain.api.auth.dto.GoogleUserInfo;
import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.api.user.service.UserService;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponse;
import uknowklp.secondbrain.global.response.BaseResponseStatus;
import uknowklp.secondbrain.global.security.jwt.JwtProvider;
import uknowklp.secondbrain.global.security.jwt.dto.CustomUserDetails;
import uknowklp.secondbrain.global.security.jwt.dto.TokenResponse;
import uknowklp.secondbrain.global.security.jwt.service.RefreshTokenService;
import uknowklp.secondbrain.global.security.oauth2.dto.AuthCodeData;
import uknowklp.secondbrain.global.security.oauth2.service.AuthorizationCodeService;
import uknowklp.secondbrain.global.security.oauth2.service.GoogleOAuth2Service;

/**
 * 인증 관련 REST API 컨트롤러
 * - Token exchange (authorization code → JWT tokens)
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
	private final AuthorizationCodeService authorizationCodeService;
	private final UserService userService;
	private final GoogleOAuth2Service googleOAuth2Service;

	@Value("${security.jwt.cookie.secure}")
	private boolean cookieSecure;

	/**
	 * Authorization Code를 JWT 토큰으로 교환
	 * OAuth2 로그인 후 프론트엔드가 받은 authorization code를 access token과 refresh token으로 교환합니다.
	 *
	 * 보안 특징:
	 * - One-time use: Code는 사용 후 즉시 삭제
	 * - Short TTL: Code는 5분 제한
	 * - Refresh Token: HttpOnly Cookie로 전달
	 * - Access Token: JSON Response로 전달
	 *
	 * @param authorizationCode OAuth2 인증 후 발급받은 authorization code (required)
	 * @param response          HTTP 응답 (쿠키 설정용)
	 * @return Access Token 정보 (JSON Body)
	 */
	@PostMapping("/token")
	public ResponseEntity<BaseResponse<TokenResponse>> exchangeToken(
		@RequestParam(name = "code", required = false) String authorizationCode,
		HttpServletResponse response) {

		// 1. Authorization Code 존재 여부 확인
		if (authorizationCode == null || authorizationCode.isEmpty()) {
			log.warn("Authorization code not provided in request");
			throw new BaseException(BaseResponseStatus.CODE_NOT_PROVIDED);
		}

		// 2. Authorization Code 검증 및 소비 (one-time use, atomic operation)
		AuthCodeData authCodeData = authorizationCodeService.validateAndConsume(authorizationCode);

		// 3. 사용자 조회
		User user = userService.findById(authCodeData.getUserId())
			.orElseThrow(() -> {
				log.error("User not found for authorization code. UserId: {}", authCodeData.getUserId());
				return new BaseException(BaseResponseStatus.USER_NOT_FOUND);
			});

		log.info("User authenticated via authorization code. UserId: {}, Email: {}",
			user.getId(), user.getEmail());

		// 4. Access Token 생성 (먼저 실행하여 실패 시 Redis 저장 방지)
		String accessToken = jwtProvider.createAccessToken(user);
		log.debug("Access token generated - UserId: {}, Email: {}", user.getId(), user.getEmail());

		// 5. Refresh Token 생성
		String refreshToken = jwtProvider.createRefreshToken(user);
		long refreshExpireSeconds = jwtProvider.getRefreshExpireTime() / 1000;

		// 6. Access Token 생성 성공 후 Refresh Token Redis 저장 (트랜잭션 일관성 보장)
		try {
			refreshTokenService.storeRefreshToken(
				user.getId(),
				refreshToken,
				refreshExpireSeconds
			);
			log.debug("Refresh token stored in Redis - UserId: {}, TTL: {}s",
				user.getId(), refreshExpireSeconds);
		} catch (Exception e) {
			log.error("Failed to store refresh token in Redis. UserId: {}, Email: {}",
				user.getId(), user.getEmail(), e);
			throw new BaseException(BaseResponseStatus.SERVER_ERROR);
		}

		// 7. Refresh Token을 HttpOnly 쿠키로 설정 (ResponseCookie 사용)
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)
			.secure(cookieSecure)
			.path("/")
			.maxAge(Duration.ofSeconds(refreshExpireSeconds))
			.sameSite("Lax")
			.build();
		response.addHeader("Set-Cookie", refreshCookie.toString());

		log.info("Token exchange successful. UserId: {}, Email: {}", user.getId(), user.getEmail());

		// 8. Access Token은 JSON Body로 응답
		TokenResponse tokenResponse = TokenResponse.of(accessToken, jwtProvider.getAccessExpireTime());
		return ResponseEntity.ok(new BaseResponse<>(tokenResponse));
	}

	/**
	 * Refresh Token으로 새로운 토큰 쌍 발급 (단순화 버전)
	 * - Token Rotation: 매 갱신 시 새로운 Refresh Token 발급
	 * - 재사용 감지 제거: 일반 웹 서비스에 적합한 수준으로 단순화
	 *
	 * @param oldRefreshToken 기존 Refresh token (쿠키에서 자동 추출)
	 * @param response HTTP 응답 (새 쿠키 설정용)
	 * @return 새로운 access token (JSON Body)
	 */
	@PostMapping("/refresh")
	public ResponseEntity<BaseResponse<TokenResponse>> refresh(
		@CookieValue(name = "refreshToken", required = false) String oldRefreshToken,
		HttpServletResponse response) {

		// 1. Refresh token 존재 여부 확인
		if (oldRefreshToken == null || oldRefreshToken.isEmpty()) {
			log.warn("Refresh token not found in cookie");
			throw new BaseException(BaseResponseStatus.REFRESH_TOKEN_NOT_FOUND);
		}

		// 2. Refresh token JWT 검증
		if (!jwtProvider.validateToken(oldRefreshToken)) {
			log.warn("Invalid refresh token");
			throw new BaseException(BaseResponseStatus.INVALID_REFRESH_TOKEN);
		}

		// 3. Claims에서 사용자 정보 추출
		Claims claims = jwtProvider.getClaims(oldRefreshToken);
		Long userId = claims.get("userId", Long.class);

		// 4. Redis에서 Refresh Token 검증 (단순 검증)
		if (!refreshTokenService.validateRefreshToken(userId, oldRefreshToken)) {
			log.warn("Refresh token validation failed. UserId: {}", userId);
			throw new BaseException(BaseResponseStatus.INVALID_REFRESH_TOKEN);
		}

		// 5. 사용자 조회
		User user = userService.findById(userId)
			.orElseThrow(() -> {
				log.error("User not found. UserId: {}", userId);
				return new BaseException(BaseResponseStatus.USER_NOT_FOUND);
			});

		// 6. 새로운 토큰 쌍 생성
		String newAccessToken = jwtProvider.createAccessToken(user);
		String newRefreshToken = jwtProvider.createRefreshToken(user);

		// 7. Redis 업데이트 (단순 교체)
		long refreshExpireSeconds = jwtProvider.getRefreshExpireTime() / 1000;
		refreshTokenService.rotateRefreshToken(userId, newRefreshToken, refreshExpireSeconds);

		// 8. 새 Refresh Token을 HttpOnly Cookie로 설정 (ResponseCookie 사용)
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
			.httpOnly(true)
			.secure(cookieSecure)
			.path("/")
			.maxAge(Duration.ofSeconds(refreshExpireSeconds))
			.sameSite("Lax")
			.build();
		response.addHeader("Set-Cookie", refreshCookie.toString());

		log.info("Token refresh successful. UserId: {}, Email: {}", userId, user.getEmail());

		// 9. Access Token은 JSON Body로 응답
		TokenResponse tokenResponse = TokenResponse.of(newAccessToken, jwtProvider.getAccessExpireTime());
		return ResponseEntity.ok(new BaseResponse<>(tokenResponse));
	}

	/**
	 * 로그아웃 처리 (단순화)
	 * - Refresh token을 Redis에서 삭제
	 * - Refresh token 쿠키 삭제
	 * - Access token은 만료까지 유효 (1시간)
	 *
	 * @param userDetails  인증된 사용자 정보
	 * @param refreshToken Refresh token (쿠키에서 자동 추출)
	 * @param response     HTTP 응답 (쿠키 삭제용)
	 * @return 성공 응답
	 */
	@PostMapping("/logout")
	public ResponseEntity<BaseResponse<Void>> logout(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@CookieValue(name = "refreshToken", required = false) String refreshToken,
		HttpServletResponse response) {

		Long userId = userDetails.getUser().getId();

		// 1. Refresh token이 존재하면 Redis에서 삭제
		if (refreshToken != null) {
			try {
				refreshTokenService.revokeRefreshToken(userId);
				log.debug("Refresh token revoked from Redis. UserId: {}", userId);
			} catch (Exception e) {
				// 이미 삭제되었거나 만료된 경우 무시
				log.debug("Failed to revoke refresh token, may be already expired. UserId: {}", userId);
			}
		}

		// 2. Refresh token 쿠키 삭제 (ResponseCookie 사용)
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
			.httpOnly(true)
			.secure(cookieSecure)
			.path("/")
			.maxAge(0)  // 즉시 삭제
			.sameSite("Lax")
			.build();
		response.addHeader("Set-Cookie", refreshCookie.toString());

		log.info("User logged out successfully. UserId: {}, Email: {}", userId, userDetails.getUsername());

		return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS));
	}

	/**
	 * Google Authorization Code를 JWT 토큰으로 교환 (Chrome Extension용)
	 *
	 * Extension이 Google OAuth에서 직접 받은 authorization code를 처리합니다.
	 *
	 * Flow:
	 * 1. Google Token Exchange API 호출하여 access token 획득
	 * 2. Google UserInfo API로 사용자 정보 조회
	 * 3. DB에서 사용자 조회 또는 생성
	 * 4. Backend JWT 토큰 생성 및 발급
	 *
	 * @param request Google token request (code, redirectUri)
	 * @param response HTTP 응답 (쿠키 설정용)
	 * @return Access Token 정보 (JSON Body)
	 */
	@PostMapping("/token/google")
	public ResponseEntity<BaseResponse<TokenResponse>> exchangeGoogleToken(
		@RequestBody @Valid GoogleTokenRequest request,
		HttpServletResponse response) {

		log.info("Processing Google token exchange for Chrome Extension");
		log.debug("Redirect URI: {}", request.getRedirectUri());

		try {
			// 1. Google Token Exchange
			GoogleTokenResponse googleTokens = googleOAuth2Service.exchangeAuthorizationCode(
				request.getCode(),
				request.getRedirectUri()
			);

			// 2. Google UserInfo 조회
			GoogleUserInfo userInfo = googleOAuth2Service.getUserInfo(googleTokens.getAccessToken());

			// 3. 사용자 조회 또는 생성 (기존 코드 재사용)
			User user = userService.saveOrUpdate(
				userInfo.getEmail(),
				userInfo.getName(),
				userInfo.getPicture()
			);

			log.info("User authenticated via Google OAuth. UserId: {}, Email: {}",
				user.getId(), user.getEmail());

			// 4. Access Token 생성
			String accessToken = jwtProvider.createAccessToken(user);
			log.debug("Access token generated for user: {}", user.getEmail());

			// 5. Refresh Token 생성
			String refreshToken = jwtProvider.createRefreshToken(user);
			long refreshExpireSeconds = jwtProvider.getRefreshExpireTime() / 1000;

			// 6. Refresh Token Redis 저장
			try {
				refreshTokenService.storeRefreshToken(
					user.getId(),
					refreshToken,
					refreshExpireSeconds
				);
				log.debug("Refresh token stored in Redis. UserId: {}", user.getId());
			} catch (Exception e) {
				log.error("Failed to store refresh token in Redis. UserId: {}", user.getId(), e);
				throw new BaseException(BaseResponseStatus.SERVER_ERROR);
			}

			// 7. Refresh Token을 HttpOnly 쿠키로 설정
			ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
				.httpOnly(true)
				.secure(cookieSecure)
				.path("/")
				.maxAge(Duration.ofSeconds(refreshExpireSeconds))
				.sameSite("Lax")
				.build();
			response.addHeader("Set-Cookie", refreshCookie.toString());

			log.info("Google token exchange successful. UserId: {}, Email: {}",
				user.getId(), user.getEmail());

			// 8. Access Token 응답
			TokenResponse tokenResponse = TokenResponse.of(accessToken, jwtProvider.getAccessExpireTime());
			return ResponseEntity.ok(new BaseResponse<>(tokenResponse));

		} catch (BaseException e) {
			log.error("Google OAuth authentication failed: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Unexpected error during Google OAuth authentication", e);
			throw new BaseException(BaseResponseStatus.SERVER_ERROR);
		}
	}
}