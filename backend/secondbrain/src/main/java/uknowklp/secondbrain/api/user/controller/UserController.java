package uknowklp.secondbrain.api.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.api.user.dto.UserResponse;
import uknowklp.secondbrain.global.security.jwt.dto.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 정보 관련 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	/**
	 * 현재 인증된 사용자의 정보 조회
	 * JWT 토큰으로 인증된 사용자 정보를 반환
	 *
	 * @param userDetails Spring Security의 인증된 사용자 정보
	 * @return ResponseEntity<UserResponse> 사용자 정보 DTO
	 */
	@GetMapping("/me")
	public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("Fetching current user info - Email: {}", userDetails.getUsername());

		User user = userDetails.getUser();
		UserResponse response = UserResponse.from(user);

		return ResponseEntity.ok(response);
	}
}
