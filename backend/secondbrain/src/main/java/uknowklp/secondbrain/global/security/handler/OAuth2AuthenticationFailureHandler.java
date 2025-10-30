package uknowklp.secondbrain.global.security.handler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.global.response.BaseResponseStatus;

/**
 * OAuth2 인증 실패 시 처리하는 핸들러
 * 사용자에게 명확한 에러 메시지와 함께 실패 페이지로 리다이렉트
 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	@Value("${secondbrain.oauth2.failure-redirect-url}")
	private String failureRedirectUrl;

	/**
	 * OAuth2 인증 실패 시 호출되는 메서드
	 * 에러 메시지를 URL 파라미터로 전달하여 프론트엔드에서 사용자에게 표시 가능
	 *
	 * @param request HTTP 요청
	 * @param response HTTP 응답
	 * @param exception 인증 실패 예외
	 */
	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException exception) throws IOException, ServletException {

		// 예외 메시지 로깅
		log.error("OAuth2 authentication failed: {}", exception.getMessage(), exception);

		// 에러 메시지를 URL 인코딩하여 안전하게 전달
		String errorMessage = URLEncoder.encode(
			getErrorMessage(exception),
			StandardCharsets.UTF_8
		);

		// 실패 리다이렉트 URL 생성
		String targetUrl = UriComponentsBuilder.fromUriString(failureRedirectUrl)
			.queryParam("error", errorMessage)
			.build()
			.toUriString();

		log.info("Redirecting to failure URL: {}", targetUrl);

		// 실패 페이지로 리다이렉트
		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}

	/**
	 * 예외 타입에 따라 사용자 친화적인 에러 메시지 생성
	 *
	 * @param exception 인증 예외
	 * @return 사용자에게 표시할 에러 메시지
	 */
	private String getErrorMessage(AuthenticationException exception) {
		String message = exception.getMessage();

		// OAuth2 인증 거부 (사용자가 권한 승인 거부)
		if (message != null && message.contains("access_denied")) {
			return BaseResponseStatus.OAUTH_ACCESS_DENIED.getMessage();
		}

		// OAuth2 제공자 연결 실패
		if (message != null && message.contains("server_error")) {
			return BaseResponseStatus.OAUTH_SERVER_ERROR.getMessage();
		}

		// 기타 OAuth2 에러
		return BaseResponseStatus.OAUTH_UNKNOWN_ERROR.getMessage();
	}
}
