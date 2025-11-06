package uknowklp.secondbrain.api.note.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.note.domain.Note;
import uknowklp.secondbrain.api.note.domain.NoteDraft;
import uknowklp.secondbrain.api.note.dto.NoteDeleteRequest;
import uknowklp.secondbrain.api.note.dto.NoteRecentResponse;
import uknowklp.secondbrain.api.note.dto.NoteReminderResponse;
import uknowklp.secondbrain.api.note.dto.NoteRequest;
import uknowklp.secondbrain.api.note.dto.NoteResponse;
import uknowklp.secondbrain.api.note.service.NoteDraftService;
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
	private final NoteDraftService noteDraftService;

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
	@Operation(summary = "노트 생성", description = "새로운 노트를 생성합니다 (제목, 내용, 이미지 업로드 지원)")
	public ResponseEntity<BaseResponse<Void>> createNote(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@RequestParam String title,
		@RequestParam String content,
		@RequestParam(required = false) List<MultipartFile> images) {

		User user = userDetails.getUser();
		int imageCount = images != null ? images.size() : 0;
		log.info("Creating note for userId: {} - Title: {}, Content length: {}, Image count: {}",
			user.getId(), title, content.length(), imageCount);

		NoteRequest request = NoteRequest.of(title, content, images);
		noteService.createNote(user.getId(), request);

		BaseResponse<Void> response = new BaseResponse<>(BaseResponseStatus.CREATED);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * 노트 조회
	 * JWT 토큰으로 인증된 사용자의 노트를 조회
	 * 본인의 노트만 조회 가능 (다른 사용자의 노트는 접근 거부)
	 *
	 * @param userDetails Spring Security의 인증된 사용자 정보
	 * @param noteId 조회할 노트 ID (URL 경로에서 추출)
	 * @return ResponseEntity<BaseResponse < NoteResponse>> 200 OK 응답 + 노트 정보
	 */
	@GetMapping("/{noteId}")
	@Operation(summary = "노트 조회", description = "노트 ID로 특정 노트의 상세 정보를 조회합니다")
	public ResponseEntity<BaseResponse<NoteResponse>> getNote(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable Long noteId) {

		User user = userDetails.getUser();
		log.info("Getting note for userId: {} - NoteId: {}", user.getId(), noteId);

		// Service에서 노트 조회 (권한 검증 포함)
		NoteResponse noteResponse = noteService.getNoteById(noteId, user.getId());

		// 200 OK 응답 생성 및 반환
		BaseResponse<NoteResponse> response = new BaseResponse<>(noteResponse);
		return ResponseEntity.ok(response);
	}

	/**
	 * 노트 수정
	 * JWT 토큰으로 인증된 사용자의 노트를 수정
	 * 본인의 노트만 수정 가능 (다른 사용자의 노트는 접근 거부)
	 * multipart/form-data 형식으로 제목, 내용, 이미지 파일들을 받음
	 *
	 * @param userDetails Spring Security의 인증된 사용자 정보
	 * @param noteId 수정할 노트 ID (URL 경로에서 추출)
	 * @param title 노트 제목
	 * @param content 노트 내용
	 * @param images 이미지 파일 목록 (optional)
	 * @return ResponseEntity<BaseResponse < NoteResponse>> 200 OK 응답 + 수정된 노트 정보
	 */
	@PutMapping(value = "/{noteId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "노트 수정", description = "기존 노트의 제목, 내용, 이미지를 수정합니다")
	public ResponseEntity<BaseResponse<NoteResponse>> updateNote(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable Long noteId,
		@RequestParam String title,
		@RequestParam String content,
		@RequestParam(required = false) List<MultipartFile> images) {

		User user = userDetails.getUser();
		log.info("Updating note for userId: {} - NoteId: {}, Title: {}, Content length: {}, Image count: {}",
			user.getId(), noteId, title, content.length(), images != null ? images.size() : 0);

		NoteRequest request = NoteRequest.of(title, content, images);
		NoteResponse noteResponse = noteService.updateNote(noteId, user.getId(), request);

		BaseResponse<NoteResponse> response = new BaseResponse<>(noteResponse);
		return ResponseEntity.ok(response);
	}

	/**
	 * 노트 삭제 (단일 및 다중 삭제 지원)
	 * JWT 토큰으로 인증된 사용자의 노트를 삭제
	 * 본인의 노트만 삭제 가능 (다른 사용자의 노트는 접근 거부)
	 * 요청 본문에 삭제할 노트 ID 목록을 전달
	 *
	 * @param userDetails Spring Security의 인증된 사용자 정보
	 * @param request 삭제할 노트 ID 목록을 담은 요청 DTO
	 * @return ResponseEntity<BaseResponse < Void>> 200 OK 응답
	 */
	@DeleteMapping
	@Operation(summary = "노트 삭제", description = "노트를 삭제합니다 (단일 및 다중 삭제 지원)")
	public ResponseEntity<BaseResponse<Void>> deleteNotes(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody NoteDeleteRequest request) {

		User user = userDetails.getUser();
		log.info("Deleting notes for userId: {} - Note count: {}", user.getId(), request.getNoteIds().size());

		noteService.deleteNotes(request.getNoteIds(), user.getId());

		BaseResponse<Void> response = new BaseResponse<>(BaseResponseStatus.SUCCESS);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{noteId}/reminders")
	@Operation(summary = "개별 노트 리마인더 활성화", description = "망각 곡선 기반 3회 리마인더 발송")
	public BaseResponse<Void> enableNoteReminder(
		@PathVariable Long noteId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		noteService.enableNoteReminder(noteId, userDetails.getUser().getId());
		return new BaseResponse<>(BaseResponseStatus.SUCCESS);
	}

	@DeleteMapping("/{noteId}/reminders")
	@Operation(summary = "개별 노트 리마인더 비활성화", description = "해당 노트 리마인더 중단하고 리마인드 횟수 0으로 초기화")
	public BaseResponse<Void> disableNoteReminder(
		@PathVariable Long noteId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		noteService.disableNoteReminder(noteId, userDetails.getUser().getId());
		return new BaseResponse<>(BaseResponseStatus.SUCCESS);
	}

	/**
	 * 최근 노트 목록 조회 (상위 10개)
	 * JWT 토큰으로 인증된 사용자의 최근 노트 목록을 조회
	 * updatedAt 기준 내림차순, 동일 시 noteId 기준 내림차순 정렬
	 *
	 * @param userDetails Spring Security의 인증된 사용자 정보
	 * @return ResponseEntity<BaseResponse < List < NoteRecentResponse>>> 200 OK 응답 + 노트 목록 (null 가능)
	 */
	@GetMapping("/recent")
	@Operation(summary = "최근 노트 목록 조회", description = "최근 수정된 노트 상위 10개를 조회합니다")
	public ResponseEntity<BaseResponse<List<NoteRecentResponse>>> getRecentNotes(
		@AuthenticationPrincipal CustomUserDetails userDetails) {

		User user = userDetails.getUser();
		log.info("Getting recent notes for userId: {}", user.getId());

		// Service에서 최근 노트 목록 조회
		List<NoteRecentResponse> recentNotes = noteService.getRecentNotes(user.getId());

		// 200 OK 응답 생성 및 반환 (data는 null 가능)
		BaseResponse<List<NoteRecentResponse>> response = new BaseResponse<>(recentNotes);
		return ResponseEntity.ok(response);
	}

	/**
	 * 리마인더가 켜진 노트 목록 조회 (페이징 지원)
	 * JWT 토큰으로 인증된 사용자의 리마인더가 활성화된 노트 목록을 조회
	 * updatedAt 기준 내림차순, 동일 시 noteId 기준 내림차순 정렬
	 * 무한스크롤을 위한 페이지네이션 지원
	 *
	 * @param userDetails Spring Security의 인증된 사용자 정보
	 * @param page 페이지 번호 (0부터 시작, 기본값: 0)
	 * @param size 페이지당 노트 개수 (기본값: 10)
	 * @return ResponseEntity<BaseResponse<NoteReminderResponse>> 200 OK 응답 + 노트 목록 (페이징 정보 포함)
	 */
	@GetMapping("/reminders")
	@Operation(summary = "리마인더 활성화 노트 목록 조회", description = "리마인더가 켜진 노트 목록 조회 (무한스크롤 지원)")
	public ResponseEntity<BaseResponse<NoteReminderResponse>> getReminderNotes(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size) {

		User user = userDetails.getUser();
		log.info("Getting reminder notes for userId: {} - page: {}, size: {}", user.getId(), page, size);

		// Service에서 리마인더 노트 목록 조회
		NoteReminderResponse reminderNotes = noteService.getReminderNotes(user.getId(), page, size);

		// 200 OK 응답 생성 및 반환
		BaseResponse<NoteReminderResponse> response = new BaseResponse<>(reminderNotes);
		return ResponseEntity.ok(response);
	}

	/**
	 * Draft에서 노트 생성 (Promote Draft to DB)
	 *
	 * 자동 저장 트리거 (명시적 "저장" 버튼 없음):
	 * 1. 프론트엔드 Batching (50회 변경 or 5분 경과)
	 * 2. 페이지 이탈 (beforeunload)
	 * 3. Side Peek 닫기
	 * 4. 백엔드 스케줄러 (5분마다)
	 *
	 * @param userDetails 인증된 사용자 정보
	 * @param noteId      Draft의 noteId (UUID)
	 * @return 생성된 Note 정보
	 */
	@PostMapping("/from-draft/{noteId}")
	@Operation(summary = "Draft 자동 저장", description = "임시 저장된 Draft를 DB에 영구 저장 (Auto-save)")
	public ResponseEntity<BaseResponse<NoteResponse>> createNoteFromDraft(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable String noteId) {

		User user = userDetails.getUser();
		log.info("Draft → DB 자동 저장 요청 - UserId: {}, NoteId: {}", user.getId(), noteId);

		// 1. Draft 조회
		NoteDraft draft = noteDraftService.getDraft(noteId, user.getId());

		// 2. Draft → NoteRequest 변환
		NoteRequest request = NoteRequest.of(
			draft.getTitle(),
			draft.getContent(),
			null // images
		);

		// 3. 기존 검증 로직 적용 (title과 content 모두 필수)
		// validateNoteRequest()에서 빈 값 체크
		Note note = noteService.createNote(user.getId(), request);

		// 4. Draft 삭제 (DB 저장 성공 후)
		noteDraftService.deleteDraft(noteId, user.getId());

		// 5. 응답 반환
		NoteResponse response = NoteResponse.from(note);
		log.info("Draft → DB 자동 저장 완료 - DraftId: {} → NoteId: {}", noteId, note.getId());

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(new BaseResponse<>(response));
	}
}
