package uknowklp.secondbrain.global.security.handler;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
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

		// 1. 사용자 조회 (CustomOAuth2UserService에서 이미 저장했으므로 조회만)
		User user = userService.findByEmail(email)
			.orElseThrow(() -> {
				log.error("User not found after OAuth2 login. Email: {}", email);
				return new AuthenticationServiceException("User not found after successful OAuth2 login");
			});

		log.info("OAuth2 authentication successful for user: {}", user.getEmail());

		// 2. JWT 토큰 생성 (Access + Refresh)
		String accessToken = jwtProvider.createAccessToken(user);
		String refreshToken = jwtProvider.createRefreshToken(user);

		log.debug("JWT tokens generated - UserId: {}, Email: {}", user.getId(), user.getEmail());

		// 3. Refresh Token을 Redis에 저장 (단순화: tokenId 제거)
		long refreshExpireSeconds = jwtProvider.getRefreshExpireTime() / 1000;
		try {
			refreshTokenService.storeRefreshToken(
				user.getId(),
				refreshToken,
				refreshExpireSeconds
			);
			log.debug("Refresh token stored in Redis - UserId: {}, TTL: {}s", user.getId(), refreshExpireSeconds);
		} catch (Exception e) {
			log.error("Failed to store refresh token in Redis. UserId: {}, Email: {}",
				user.getId(), user.getEmail(), e);
			throw new AuthenticationServiceException(
				"Unable to complete authentication due to system error", e);
		}

		// 4. Refresh Token을 HttpOnly 쿠키로 설정 (ResponseCookie 사용)
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)                                     // XSS 보호
			.secure(cookieSecure)                               // HTTPS 전용
			.path("/")                                          // 모든 경로에서 사용
			.maxAge(Duration.ofSeconds(refreshExpireSeconds))   // 7일
			.sameSite("Lax")                                    // CSRF 보호
			.build();
		response.addHeader("Set-Cookie", refreshCookie.toString());

		// 5. SPA를 위한 응답 처리 - 항상 JSON으로 통일
		// Access Token은 Response Body로만 전달 (2024 Best Practice)

		// 리다이렉트 URL을 포함한 성공 페이지로 이동
		// 프론트엔드에서 이 페이지에서 postMessage로 토큰을 부모 창에 전달
		String successPageUrl = redirectUrl + "/auth/callback";

		// HTML 페이지로 리다이렉트 (토큰은 JavaScript로 처리)
		response.setContentType("text/html;charset=UTF-8");
		response.getWriter().write(
			"<!DOCTYPE html>" +
			"<html>" +
			"<head>" +
			"    <title>Login Success</title>" +
			"    <script>" +
			"        window.onload = function() {" +
			"            // 부모 창에 인증 성공 메시지와 토큰 전달" +
			"            if (window.opener) {" +
			"                window.opener.postMessage({" +
			"                    type: 'auth-success'," +
			"                    accessToken: '" + accessToken + "'," +
			"                    tokenType: 'Bearer'," +
			"                    expiresIn: " + (jwtProvider.getAccessExpireTime() / 1000) +
			"                }, '" + redirectUrl + "');" +
			"                window.close();" +
			"            } else {" +
			"                // 팝업이 아닌 경우 리다이렉트" +
			"                window.location.href = '" + redirectUrl + "';" +
			"            }" +
			"        };" +
			"    </script>" +
			"</head>" +
			"<body>" +
			"    <p>Login successful! Redirecting...</p>" +
			"</body>" +
			"</html>"
		);

		log.info("OAuth2 login successful. Access token delivered via postMessage for SPA");
	}
}
