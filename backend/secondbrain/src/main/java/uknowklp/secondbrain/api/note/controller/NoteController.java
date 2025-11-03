package uknowklp.secondbrain.api.note.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
	 * S3 연결 전까지는 더미 URL로 이미지 마크다운 생성
	 *
	 * @param userDetails Spring Security의 인증된 사용자 정보
	 * @param title 노트 제목
	 * @param content 노트 내용
	 * @param images 이미지 파일 목록 (optional)
	 * @return ResponseEntity<BaseResponse> 201 Created 응답
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<BaseResponse<Void>> createNote(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@RequestParam @NotBlank(message = "제목은 필수입니다.") @Size(max = 64, message = "제목은 최대 64자까지 입력 가능합니다.") String title,
		@RequestParam @NotBlank(message = "내용은 필수입니다.") @Size(max = 2048, message = "내용은 최대 2048자까지 입력 가능합니다.") String content,
		@RequestParam(required = false) List<MultipartFile> images) {

		User user = userDetails.getUser();
		int imageCount = images != null ? images.size() : 0;
		log.info("Creating note for userId: {} - Title: {}, Content length: {}, Image count: {}",
			user.getId(), title, content.length(), imageCount);

		// NoteRequest 객체 생성 (빌더 패턴 사용)
		NoteRequest request = NoteRequest.builder()
			.title(title)
			.content(content)
			.images(images)
			.build();

		noteService.createNote(user.getId(), request);

		BaseResponse<Void> response = new BaseResponse<>(BaseResponseStatus.CREATED);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
