package uknowklp.secondbrain.api.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

// 모바일 앱에서 Google ID Token을 받는 요청 DTO
// 웹은 OAuth2 플로우를 사용하지만, 모바일은 Google Sign-In SDK를 사용하므로 별도 엔드포인트 필요
@Getter
@NoArgsConstructor
public class GoogleAuthRequest {
	private String idToken; // Google Sign-In SDK에서 받은 ID Token
}
