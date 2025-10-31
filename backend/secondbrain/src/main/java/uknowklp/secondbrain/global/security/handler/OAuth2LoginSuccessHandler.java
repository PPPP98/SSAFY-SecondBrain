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
import uknowklp.secondbrain.global.security.oauth2.service.AuthorizationCodeService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final AuthorizationCodeService authorizationCodeService;
	private final UserService userService;

	@Value("${secondbrain.oauth2.redirect-url}")
	private String redirectUrl;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {

		OAuth2User oAuth2User = (OAuth2User)authentication.getPrincipal();
		String email = oAuth2User.getAttribute("email");

		// 0. 이메일 null/empty 체크 (OAuth2 Provider에서 이메일 제공하지 않은 경우 방어)
		if (email == null || email.isEmpty()) {
			log.error("Email not provided by OAuth2 provider");
			throw new AuthenticationServiceException("Email is required for authentication");
		}

		// 1. 사용자 조회 (CustomOAuth2UserService에서 이미 저장했으므로 조회만)
		User user = userService.findByEmail(email)
			.orElseThrow(() -> {
				log.error("User not found after OAuth2 login. Email: {}", email);
				return new AuthenticationServiceException("User not found after successful OAuth2 login");
			});

		log.info("OAuth2 authentication successful for user: {}", user.getEmail());

		// 2. Authorization Code 생성 및 Redis 저장
		String authorizationCode = authorizationCodeService.generateCode(user.getId(), user.getEmail());

		log.debug("Authorization code generated - UserId: {}, Email: {}", user.getId(), user.getEmail());

		// 3. 프론트엔드 Callback URL로 리다이렉트 with code
		String callbackUrl = UriComponentsBuilder
			.fromUriString(redirectUrl + "/auth/callback")
			.queryParam("code", authorizationCode)
			.build()
			.toUriString();

		log.info("Redirecting to frontend callback with authorization code. URL: {}", callbackUrl);
		response.sendRedirect(callbackUrl);
	}
}
