package uknowklp.secondbrain.api.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.api.user.dto.UserResponse;
import uknowklp.secondbrain.api.user.service.UserService;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponse;
import uknowklp.secondbrain.global.response.BaseResponseStatus;
import uknowklp.secondbrain.global.security.jwt.dto.CustomUserDetails;

/**
 * 사용자 정보 관련 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	// 현재 인증된 사용자의 정보 조회
	@GetMapping("/me")
	@Operation(summary = "현재 사용자 정보 조회", description = "JWT 토큰으로 인증된 사용자의 정보를 조회합니다")
	public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("Fetching current user info - Email: {}", userDetails.getUsername());

		// DB에서 최신 사용자 정보 조회
		User user = userService.findById(userDetails.getUser().getId())
			.orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

		UserResponse response = UserResponse.from(user);
		log.debug("User info retrieved - Name: {}, Picture: {}", user.getName(), user.getPicture());

		return ResponseEntity.ok(response);
	}

	// 사용자 리마인더 알람 토글
	@PostMapping("/reminders")
	@Operation(summary = "리마인더 알람 토글", description = "사용자의 전체 리마인더 알람을 On/Off 토글합니다")
	public ResponseEntity<BaseResponse<Void>> toggleReminder(@AuthenticationPrincipal CustomUserDetails userDetails) {
		User user = userDetails.getUser();
		log.info("Toggling reminder alarm for userId: {}", user.getId());

		// Service 호출하여 알람 상태 토글
		userService.toggleSetAlarm(user.getId());

		// 성공 응답 반환
		BaseResponse<Void> response = new BaseResponse<>(BaseResponseStatus.SUCCESS);
		return ResponseEntity.ok(response);
	}
}
