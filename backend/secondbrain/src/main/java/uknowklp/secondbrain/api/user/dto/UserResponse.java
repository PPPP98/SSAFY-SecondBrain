package uknowklp.secondbrain.api.user.dto;

import uknowklp.secondbrain.api.user.domain.User;

import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 정보 응답 DTO
 * 클라이언트에 전달할 사용자 정보를 담는 객체
 */
@Getter
@Builder
public class UserResponse {

	private Long id;
	private String email;
	private String name;
	private String picture;
	private Boolean setAlarm;

	/**
	 * User 엔티티로부터 UserResponse 생성
	 *
	 * @param user 사용자 엔티티
	 * @return UserResponse DTO
	 */
	public static UserResponse from(User user) {
		return UserResponse.builder()
			.id(user.getId())
			.email(user.getEmail())
			.name(user.getName())
			.picture(user.getPicture())
			.setAlarm(user.getSetAlarm())
			.build();
	}
}
