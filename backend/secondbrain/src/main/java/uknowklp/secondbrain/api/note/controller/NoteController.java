package uknowklp.secondbrain.api.note.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.note.dto.NoteRequest;
import uknowklp.secondbrain.api.note.service.NoteService;
import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.global.response.BaseResponse;
import uknowklp.secondbrain.global.response.BaseResponseStatus;
import uknowklp.secondbrain.global.security.jwt.dto.CustomUserDetails;

/**
 * 노트 관련 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

	private final NoteService noteService;

	/**
	 * 새로운 노트 생성
	 * JWT 토큰으로 인증된 사용자의 노트를 생성
	 *
	 * @param userDetails Spring Security의 인증된 사용자 정보
	 * @param request 노트 생성 요청 DTO (제목, 내용)
	 * @return ResponseEntity<BaseResponse> 201 Created 응답
	 */
	@PostMapping
	public ResponseEntity<BaseResponse<Void>> createNote(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody NoteRequest request) {

		User user = userDetails.getUser();
		log.info("Creating note for userId: {} - Title: {}", user.getId(), request.getTitle());
		noteService.createNote(user.getId(), request);

		BaseResponse<Void> response = new BaseResponse<>(BaseResponseStatus.CREATED);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
