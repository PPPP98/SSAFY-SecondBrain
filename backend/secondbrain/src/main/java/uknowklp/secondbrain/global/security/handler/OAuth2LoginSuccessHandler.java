package uknowklp.secondbrain.global.security.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.api.user.service.UserService;
import uknowklp.secondbrain.global.security.jwt.JwtProvider;

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

	@Value("${secondbrain.oauth2.redirect-url}")
	private String redirectUrl;

	@Value("${security.jwt.cookie.secure}")
	private boolean cookieSecure;

	private final UserService userService;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {
		OAuth2User oAuth2User = (OAuth2User)authentication.getPrincipal();
		String email = oAuth2User.getAttribute("email");

		User user = userService.saveOrUpdate(email,
			oAuth2User.getAttribute("name"),
			oAuth2User.getAttribute("picture"));

		String accessToken = jwtProvider.createToken(user);
		log.info("JWT token generated for user: {}", user.getEmail());

		// HttpOnly 쿠키에 JWT 토큰 저장 (보안 강화)
		Cookie jwtCookie = new Cookie("accessToken", accessToken);
		jwtCookie.setHttpOnly(true);  // JavaScript 접근 차단
		jwtCookie.setSecure(cookieSecure);  // 환경별 설정 (프로덕션: true, 로컬: false)
		jwtCookie.setPath("/");        // 모든 경로에서 전송
		jwtCookie.setMaxAge(60 * 60 * 24 * 21);  // 21일 (JWT 만료 시간과 일치)
		response.addCookie(jwtCookie);

		log.debug("JWT cookie set - Secure: {}, MaxAge: {} days", cookieSecure, 21);

		// 로그인 성공 여부만 전달 (사용자 정보는 /api/users/me로 조회)
		String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
			.queryParam("loginSuccess", "true")
			.build()
			.toUriString();

		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}
}
