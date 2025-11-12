package uknowklp.secondbrain.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Google UserInfo Response DTO
 * Google OAuth2 UserInfo Endpoint 응답 구조
 */
@Getter
@NoArgsConstructor
@ToString
public class GoogleUserInfo {

	private String sub;  // Google User ID

	private String email;

	private String name;

	private String picture;

	@JsonProperty("email_verified")
	private Boolean emailVerified;

	@JsonProperty("given_name")
	private String givenName;

	@JsonProperty("family_name")
	private String familyName;
}
