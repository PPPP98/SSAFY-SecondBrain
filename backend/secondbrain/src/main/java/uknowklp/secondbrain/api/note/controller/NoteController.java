package uknowklp.secondbrain.api.note.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
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
	 * multipart/form-data 형식으로 제목, 내용, 이미지 파일들을 받음
	 *
	 * @param userDetails Spring Security의 인증된 사용자 정보
	 * @param request 노트 생성 요청 DTO (제목, 내용, 이미지 파일 목록)
	 * @return ResponseEntity<BaseResponse> 201 Created 응답
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<BaseResponse<Void>> createNote(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @ModelAttribute NoteRequest request) {

		User user = userDetails.getUser();
		int imageCount = request.getImages() != null ? request.getImages().size() : 0;
		log.info("Creating note for userId: {} - Title: {}, Image count: {}",
			user.getId(), request.getTitle(), imageCount);

		noteService.createNote(user.getId(), request);

		BaseResponse<Void> response = new BaseResponse<>(BaseResponseStatus.CREATED);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
