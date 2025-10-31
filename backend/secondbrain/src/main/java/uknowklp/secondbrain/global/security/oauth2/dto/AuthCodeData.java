package uknowklp.secondbrain.global.security.oauth2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Authorization Code 데이터 DTO
 * Redis에 저장되는 인증 코드 관련 정보를 담는 객체
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthCodeData {

	/**
	 * 사용자 ID
	 */
	private Long userId;

	/**
	 * 사용자 이메일
	 */
	private String email;

	/**
	 * 코드 생성 시각 (밀리초 단위 타임스탬프)
	 */
	private Long createdAt;
}
