package uknowklp.secondbrain.global.security.oauth2.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponseStatus;

// Google ID Token 검증 서비스 (모바일 앱용)
@Slf4j
@Service
public class GoogleTokenVerifier {

	@Value("${spring.security.oauth2.client.registration.google.client-id}")
	private String clientId;

	@Value("${GOOGLE_ANDROID_CLIENT_ID:#{null}}")
	private String androidClientId;

	// Google ID Token 검증 및 이메일 추출
	public String verifyIdToken(String idTokenString) {
		try {
			// 여러 Client ID를 지원하기 위한 리스트 생성
			java.util.List<String> clientIds = new java.util.ArrayList<>();
			clientIds.add(clientId);
			if (androidClientId != null && !androidClientId.isEmpty()) {
				clientIds.add(androidClientId);
			}

			// Google ID Token Verifier 생성
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
				new NetHttpTransport(),
				GsonFactory.getDefaultInstance())
				.setAudience(clientIds)
				.build();

			// ID Token 검증
			GoogleIdToken idToken = verifier.verify(idTokenString);

			if (idToken != null) {
				GoogleIdToken.Payload payload = idToken.getPayload();

				// 이메일 추출
				String email = payload.getEmail();
				boolean emailVerified = payload.getEmailVerified();

				if (!emailVerified) {
					log.warn("Email not verified for Google ID Token");
					throw new BaseException(BaseResponseStatus.INVALID_GOOGLE_TOKEN);
				}

				log.info("Google ID Token verified successfully. Email: {}", email);
				return email;
			} else {
				log.warn("Invalid Google ID Token");
				throw new BaseException(BaseResponseStatus.INVALID_GOOGLE_TOKEN);
			}
		} catch (GeneralSecurityException | IOException e) {
			log.error("Failed to verify Google ID Token", e);
			throw new BaseException(BaseResponseStatus.INVALID_GOOGLE_TOKEN);
		}
	}
}
