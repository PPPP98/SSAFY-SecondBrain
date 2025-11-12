package uknowklp.secondbrain.global.security.oauth2.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.auth.dto.GoogleTokenResponse;
import uknowklp.secondbrain.api.auth.dto.GoogleUserInfo;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponseStatus;

/**
 * Google OAuth2 서비스
 * Chrome Extension의 Google OAuth flow 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuth2Service {

	private final RestTemplate restTemplate;

	@Value("${spring.security.oauth2.client.registration.google.client-id}")
	private String clientId;

	@Value("${spring.security.oauth2.client.registration.google.client-secret}")
	private String clientSecret;

	private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
	private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

	/**
	 * Google Authorization Code를 Access Token으로 교환
	 *
	 * @param code Google authorization code
	 * @param redirectUri Chrome Extension redirect URI
	 * @return GoogleTokenResponse
	 */
	public GoogleTokenResponse exchangeAuthorizationCode(String code, String redirectUri) {
		log.info("Exchanging Google authorization code for access token");
		log.debug("Redirect URI: {}", redirectUri);

		// Request parameters (application/x-www-form-urlencoded)
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("code", code);
		params.add("client_id", clientId);
		params.add("client_secret", clientSecret);
		params.add("redirect_uri", redirectUri);
		params.add("grant_type", "authorization_code");

		// Headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

		try {
			ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(
				GOOGLE_TOKEN_URL,
				request,
				GoogleTokenResponse.class
			);

			GoogleTokenResponse tokenResponse = response.getBody();

			if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
				log.error("Google token exchange returned null or empty response");
				throw new BaseException(BaseResponseStatus.GOOGLE_TOKEN_EXCHANGE_FAILED);
			}

			log.info("Google token exchange successful");

			return tokenResponse;

		} catch (HttpClientErrorException e) {
			log.error("Google token exchange failed with HTTP error: {} - {}",
				e.getStatusCode(), e.getResponseBodyAsString());
			throw new BaseException(BaseResponseStatus.GOOGLE_TOKEN_EXCHANGE_FAILED);
		} catch (Exception e) {
			log.error("Google token exchange failed with unexpected error", e);
			throw new BaseException(BaseResponseStatus.GOOGLE_TOKEN_EXCHANGE_FAILED);
		}
	}

	/**
	 * Google Access Token으로 사용자 정보 조회
	 *
	 * @param accessToken Google access token
	 * @return GoogleUserInfo
	 */
	public GoogleUserInfo getUserInfo(String accessToken) {
		log.info("Fetching Google user info");

		// Headers with Bearer token
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);

		HttpEntity<Void> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<GoogleUserInfo> response = restTemplate.exchange(
				GOOGLE_USERINFO_URL,
				HttpMethod.GET,
				request,
				GoogleUserInfo.class
			);

			GoogleUserInfo userInfo = response.getBody();

			if (userInfo == null || userInfo.getEmail() == null) {
				log.error("Google user info returned null or empty email");
				throw new BaseException(BaseResponseStatus.GOOGLE_USER_INFO_FAILED);
			}

			log.info("Google user info fetched successfully. Email: {}", userInfo.getEmail());

			return userInfo;

		} catch (HttpClientErrorException e) {
			log.error("Failed to fetch Google user info with HTTP error: {} - {}",
				e.getStatusCode(), e.getResponseBodyAsString());
			throw new BaseException(BaseResponseStatus.GOOGLE_USER_INFO_FAILED);
		} catch (Exception e) {
			log.error("Failed to fetch Google user info with unexpected error", e);
			throw new BaseException(BaseResponseStatus.GOOGLE_USER_INFO_FAILED);
		}
	}
}
