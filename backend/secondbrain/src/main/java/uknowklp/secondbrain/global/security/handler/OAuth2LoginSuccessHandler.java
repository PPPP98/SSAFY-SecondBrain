package uknowklp.secondbrain.global.security.handler;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.api.user.service.UserService;
import uknowklp.secondbrain.global.security.jwt.JwtProvider;
import uknowklp.secondbrain.global.security.jwt.service.RefreshTokenService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final JwtProvider jwtProvider;
	private final RefreshTokenService refreshTokenService;
	private final RedisTemplate<String, Object> redisTemplate;
	private final UserService userService;

	@Value("${secondbrain.oauth2.redirect-url}")
	private String redirectUrl;

	@Value("${security.jwt.cookie.secure}")
	private boolean cookieSecure;

	private static final String AUTH_CODE_PREFIX = "auth_code:";
	private static final Duration AUTH_CODE_TTL = Duration.ofMinutes(5);

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {
		OAuth2User oAuth2User = (OAuth2User)authentication.getPrincipal();
		String email = oAuth2User.getAttribute("email");

		User user = userService.saveOrUpdate(email,
			oAuth2User.getAttribute("name"),
			oAuth2User.getAttribute("picture"));

		// 1. Refresh Token 생성
		String refreshToken = jwtProvider.createRefreshToken(user);
		String refreshTokenId = jwtProvider.getTokenId(refreshToken);

		log.info("JWT refresh token generated for user: {}", user.getEmail());

		// 2. Refresh Token을 Redis에 저장
		long refreshExpireSeconds = jwtProvider.getRefreshExpireTime() / 1000;
		try {
			refreshTokenService.storeRefreshToken(
				String.valueOf(user.getId()),
				refreshToken,
				refreshTokenId,
				refreshExpireSeconds
			);
		} catch (Exception e) {
			log.error("Failed to store refresh token in Redis. UserId: {}, TokenId: {}, Email: {}",
				user.getId(), refreshTokenId, user.getEmail(), e);
			throw new AuthenticationServiceException(
				"Unable to complete authentication due to system error. Please try again.", e);
		}

		// 3. Refresh Token을 HttpOnly 쿠키에 저장 (보안 강화)
		Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
		refreshCookie.setHttpOnly(true);  // JavaScript 접근 차단
		refreshCookie.setSecure(cookieSecure);  // 환경별 설정 (프로덕션: true, 로컬: false)
		refreshCookie.setPath("/");        // 모든 경로에서 전송
		refreshCookie.setMaxAge((int)refreshExpireSeconds);  // 30일
		refreshCookie.setAttribute("SameSite", "Lax");  // CSRF 보호 (OAuth2 리다이렉트 지원)
		response.addCookie(refreshCookie);

		log.debug("Refresh token cookie set - Secure: {}, MaxAge: {} days", cookieSecure, refreshExpireSeconds / 86400);

		// 4. 임시 인증 코드 생성 (Authorization Code Pattern)
		String authCode = UUID.randomUUID().toString();

		// 5. Redis에 인증 코드 저장 (5분 만료, 일회용)
		String authCodeKey = AUTH_CODE_PREFIX + authCode;
		try {
			redisTemplate.opsForValue().set(
				authCodeKey,
				user.getId().toString(),
				AUTH_CODE_TTL
			);
		} catch (Exception e) {
			log.error("Failed to store authorization code in Redis. UserId: {}, Code: {}, Email: {}",
				user.getId(), authCode, user.getEmail(), e);
			throw new AuthenticationServiceException(
				"Unable to complete authentication due to system error. Please try again.", e);
		}

		log.info("Authorization code generated for user: {}, expires in 5 minutes", user.getEmail());

		// 6. 인증 코드만 URL 파라미터로 전달 (보안 강화)
		String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
			.queryParam("code", authCode)
			.build()
			.toUriString();

		log.info("OAuth2 login success. Redirecting to: {}", redirectUrl);

		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}
}
