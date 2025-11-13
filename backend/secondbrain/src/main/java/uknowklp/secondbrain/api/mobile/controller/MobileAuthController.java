package uknowklp.secondbrain.api.mobile.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.auth.dto.GoogleAuthRequest;
import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.api.user.service.UserService;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponse;
import uknowklp.secondbrain.global.response.BaseResponseStatus;
import uknowklp.secondbrain.global.security.jwt.JwtProvider;
import uknowklp.secondbrain.global.security.jwt.dto.TokenResponse;
import uknowklp.secondbrain.global.security.jwt.service.RefreshTokenService;
import uknowklp.secondbrain.global.security.oauth2.service.GoogleTokenVerifier;

@Slf4j
@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
public class MobileAuthController {

	private final GoogleTokenVerifier googleTokenVerifier;
	private final UserService userService;
	private final JwtProvider jwtProvider;
	private final RefreshTokenService refreshTokenService;

	@Value("${jwt.cookie.secure:false}")
	private boolean cookieSecure;

	/**
	 * 모바일 앱 Google ID Token 인증 및 JWT 발급
	 * - 보안 설정 없이 모든 요청 허용 (/api/mobile/** permitAll)
	 * - AuthController의 /api/auth/google과 동일한 로직
	 */
	@PostMapping("/auth/google")
	public ResponseEntity<BaseResponse<TokenResponse>> authenticateWithGoogle(
		@RequestBody GoogleAuthRequest request,
		HttpServletResponse response) {

		// 1. Google ID Token 검증 및 이메일 추출
		String email = googleTokenVerifier.verifyIdToken(request.getIdToken());

		// 2. 사용자 조회 또는 생성
		String name = email.split("@")[0];
		User user = userService.saveOrUpdate(email, name, null);

		log.info("Mobile app authentication successful. UserId: {}, Email: {}", user.getId(), user.getEmail());

		// 3. Access Token 생성
		String accessToken = jwtProvider.createAccessToken(user);
		log.debug("Access token generated for mobile app - UserId: {}, Email: {}", user.getId(), user.getEmail());

		// 4. Refresh Token 생성
		String refreshToken = jwtProvider.createRefreshToken(user);
		long refreshExpireSeconds = jwtProvider.getRefreshExpireTime() / 1000;

		// 5. Refresh Token을 Redis에 저장
		try {
			refreshTokenService.storeRefreshToken(
				user.getId(),
				refreshToken,
				refreshExpireSeconds
			);
			log.debug("Refresh token stored in Redis for mobile app - UserId: {}, TTL: {}s",
				user.getId(), refreshExpireSeconds);
		} catch (Exception e) {
			log.error("Failed to store refresh token in Redis. UserId: {}, Email: {}",
				user.getId(), user.getEmail(), e);
			throw new BaseException(BaseResponseStatus.SERVER_ERROR);
		}

		// 6. Refresh Token을 HttpOnly 쿠키로 설정 (모바일에서는 사용 안 할 수도 있음)
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)
			.secure(cookieSecure)
			.path("/")
			.maxAge(Duration.ofSeconds(refreshExpireSeconds))
			.sameSite("Lax")
			.build();
		response.addHeader("Set-Cookie", refreshCookie.toString());

		log.info("Mobile app token issuance successful. UserId: {}, Email: {}", user.getId(), user.getEmail());

		// 7. Access Token은 JSON Body로 응답
		TokenResponse tokenResponse = TokenResponse.of(accessToken, jwtProvider.getAccessExpireTime());
		return ResponseEntity.ok(new BaseResponse<>(tokenResponse));
	}
}
