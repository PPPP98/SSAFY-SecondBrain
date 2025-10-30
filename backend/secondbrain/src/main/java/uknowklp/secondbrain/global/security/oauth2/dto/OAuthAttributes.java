package uknowklp.secondbrain.global.security.oauth2.dto;

import java.util.Map;

import uknowklp.secondbrain.api.user.domain.User;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OAuthAttributes {

	// OAuth2User에서 반환하는 사용자 정보 Map
	private Map<String, Object> attributes;
	// 사용자 이름의 키 값
	private String nameAttributeKey;
	private String name;
	private String email;
	private String picture;

	public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email,
		String picture) {
		this.attributes = attributes;
		this.nameAttributeKey = nameAttributeKey;
		this.name = name;
		this.email = email;
		this.picture = picture;
	}

	// Google 로그인 사용자 정보를 바탕으로 객체 생성
	public static OAuthAttributes of(String userNameAttributeName, Map<String, Object> attributes) {
		return OAuthAttributes.builder()
			.name((String)attributes.get("name"))
			.email((String)attributes.get("email"))
			.picture((String)attributes.get("picture"))
			.attributes(attributes)
			.nameAttributeKey(userNameAttributeName)
			.build();
	}

	public User toEntity() {
		return User.builder()
			.name(name)
			.email(email)
			.picture(picture)
			.setAlarm(false)
			.build();
	}
}
