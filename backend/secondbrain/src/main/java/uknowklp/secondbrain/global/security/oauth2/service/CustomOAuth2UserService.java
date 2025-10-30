package uknowklp.secondbrain.global.security.oauth2.service;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.api.user.service.UserService;
import uknowklp.secondbrain.global.security.oauth2.dto.OAuthAttributes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 사용자 정보를 처리하는 커스텀 서비스
 * Spring Security OAuth2 권장 패턴에 따라 UserInfo Endpoint에서 사용자 정보를 로드하고 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserService userService;

	/**
	 * OAuth2 Provider로부터 사용자 정보를 로드하고 DB에 저장/업데이트
	 *
	 * @param userRequest OAuth2 사용자 요청 정보
	 * @return OAuth2User 인증된 OAuth2 사용자 객체
	 * @throws OAuth2AuthenticationException OAuth2 인증 실패 시
	 */
	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		// 1. 부모 클래스의 loadUser를 호출하여 OAuth2User 정보 가져오기
		OAuth2User oauth2User = super.loadUser(userRequest);

		// 2. OAuth2 제공자 정보 추출
		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		String userNameAttributeName = userRequest.getClientRegistration()
			.getProviderDetails()
			.getUserInfoEndpoint()
			.getUserNameAttributeName();

		log.info("OAuth2 login attempt - Provider: {}, UserNameAttribute: {}", registrationId, userNameAttributeName);

		// 3. OAuth2User 속성을 OAuthAttributes DTO로 변환
		OAuthAttributes attributes = OAuthAttributes.of(userNameAttributeName, oauth2User.getAttributes());

		// 4. 사용자 정보 저장 또는 업데이트
		User user = saveOrUpdate(attributes);

		log.info("OAuth2 user loaded successfully - Email: {}, Name: {}", user.getEmail(), user.getName());

		// 5. Spring Security가 사용할 OAuth2User 객체 반환
		return new DefaultOAuth2User(
			Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
			attributes.getAttributes(),
			attributes.getNameAttributeKey()
		);
	}

	/**
	 * 사용자 정보를 DB에 저장하거나 업데이트
	 *
	 * @param attributes OAuth2 사용자 속성
	 * @return User 저장/업데이트된 사용자 엔티티
	 */
	private User saveOrUpdate(OAuthAttributes attributes) {
		return userService.saveOrUpdate(
			attributes.getEmail(),
			attributes.getName(),
			attributes.getPicture()
		);
	}
}
