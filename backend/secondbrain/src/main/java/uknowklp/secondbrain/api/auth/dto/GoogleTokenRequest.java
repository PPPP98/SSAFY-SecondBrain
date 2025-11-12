package uknowklp.secondbrain.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Google Token Exchange Request DTO
 * Chrome Extension이 Google OAuth에서 받은 authorization code를 Backend로 전달
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleTokenRequest {

	@NotBlank(message = "Authorization code is required")
	private String code;

	@NotBlank(message = "Redirect URI is required")
	private String redirectUri;
}
