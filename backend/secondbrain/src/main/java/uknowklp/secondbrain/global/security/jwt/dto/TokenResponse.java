package uknowklp.secondbrain.global.security.jwt.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Access Token 응답 DTO
 * Refresh token은 HttpOnly 쿠키로 전달되며, Access token만 응답 body에 포함됩니다.
 */
@Getter
@AllArgsConstructor
public class TokenResponse {

	private String accessToken;
	private String tokenType;  // "Bearer"
	private Long expiresIn;    // 만료 시간 (초 단위)

	/**
	 * TokenResponse 생성 팩토리 메서드
	 *
	 * @param accessToken     생성된 access token
	 * @param expiresInMillis 만료 시간 (밀리초 단위)
	 * @return TokenResponse 객체
	 */
	public static TokenResponse of(String accessToken, long expiresInMillis) {
		return new TokenResponse(
			accessToken,
			"Bearer",
			expiresInMillis / 1000  // 밀리초를 초로 변환
		);
	}
}
