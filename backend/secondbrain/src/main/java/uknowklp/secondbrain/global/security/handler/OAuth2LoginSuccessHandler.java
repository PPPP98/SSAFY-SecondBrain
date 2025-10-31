package uknowklp.secondbrain.global.security.handler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
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
	private final UserService userService;

	@Value("${secondbrain.oauth2.redirect-url}")
	private String redirectUrl;

	@Value("${security.jwt.cookie.secure}")
	private boolean cookieSecure;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {

		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
		String email = oAuth2User.getAttribute("email");

		// 1. 사용자 정보 저장 또는 업데이트
		User user = userService.saveOrUpdate(
			email,
			oAuth2User.getAttribute("name"),
			oAuth2User.getAttribute("picture")
		);

		log.info("OAuth2 authentication successful for user: {}", user.getEmail());

		// 2. JWT 토큰 생성 (Access + Refresh)
		String accessToken = jwtProvider.createAccessToken(user);
		String refreshToken = jwtProvider.createRefreshToken(user);
		String refreshTokenId = jwtProvider.getTokenId(refreshToken);

		log.debug("JWT tokens generated - UserId: {}, Email: {}", user.getId(), user.getEmail());

		// 3. Refresh Token을 Redis에 단순 저장 (복잡한 메타데이터 제거)
		long refreshExpireSeconds = jwtProvider.getRefreshExpireTime() / 1000;
		try {
			refreshTokenService.storeRefreshToken(
				String.valueOf(user.getId()),
				refreshToken,
				refreshTokenId,
				refreshExpireSeconds
			);
			log.debug("Refresh token stored in Redis - UserId: {}, TTL: {}s", user.getId(), refreshExpireSeconds);
		} catch (Exception e) {
			log.error("Failed to store refresh token in Redis. UserId: {}, Email: {}",
				user.getId(), user.getEmail(), e);
			throw new AuthenticationServiceException(
				"Unable to complete authentication due to system error", e);
		}

		// 4. Refresh Token을 HttpOnly 쿠키로 설정
		Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
		refreshCookie.setHttpOnly(true);                        // XSS 보호
		refreshCookie.setSecure(cookieSecure);                  // HTTPS 전용
		refreshCookie.setPath("/");                             // 모든 경로에서 사용
		refreshCookie.setMaxAge((int) refreshExpireSeconds);    // 7일 (application.yml 설정값)
		refreshCookie.setAttribute("SameSite", "Lax");          // CSRF 보호
		response.addCookie(refreshCookie);

		// 5. Access Token을 URL fragment로 전달 (SPA 표준 방식)
		// Fragment (#)를 사용하면 서버로 전송되지 않아 보안상 유리
		String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
			.fragment("access_token=" + accessToken + "&token_type=Bearer&expires_in=" + (jwtProvider.getAccessExpireTime() / 1000))
			.build()
			.toUriString();

		log.info("OAuth2 login successful. Redirecting to: {} with access token in fragment", redirectUrl);

		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}
}
