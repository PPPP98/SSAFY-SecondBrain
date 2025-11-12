package uknowklp.secondbrain.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Google Token Exchange Response DTO
 * Google OAuth2 Token Endpoint 응답 구조
 */
@Getter
@NoArgsConstructor
@ToString
public class GoogleTokenResponse {

	@JsonProperty("access_token")
	private String accessToken;

	@JsonProperty("expires_in")
	private Integer expiresIn;

	@JsonProperty("refresh_token")
	private String refreshToken;

	@JsonProperty("scope")
	private String scope;

	@JsonProperty("token_type")
	private String tokenType;

	@JsonProperty("id_token")
	private String idToken;
}
