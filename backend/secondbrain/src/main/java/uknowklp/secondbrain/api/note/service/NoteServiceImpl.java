package uknowklp.secondbrain.api.note.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.note.domain.Note;
import uknowklp.secondbrain.api.note.domain.NoteDocument;
import uknowklp.secondbrain.api.note.dto.NoteRequest;
import uknowklp.secondbrain.api.note.dto.NoteResponse;
import uknowklp.secondbrain.api.note.repository.NoteRepository;
import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.api.user.service.UserService;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponseStatus;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NoteServiceImpl implements NoteService {

	private final NoteRepository noteRepository;
	private final UserService userService;
	private final NoteSearchService noteSearchService;
	// TODO: S3 업로드 서비스 추가 예정
	// private final S3UploadService s3UploadService;

	@Override
	public Note createNote(Long userId, NoteRequest request) {
		log.info("Creating note for user ID: {}", userId);

		// 요청 데이터 검증
		validateNoteRequest(request);

		// 사용자 존재 확인
		User user = userService.findById(userId)
			.orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

		// 이미지 파일 처리 및 마크다운 content 생성
		String finalContent = processImagesAndContent(request.getContent(), request.getImages());

		// 서비스 레벨 검증: 이미지 추가 후 최종 content 길이 확인
		validateContentLength(finalContent);

		// 노트 생성
		Note note = Note.builder()
			.user(user)
			.title(request.getTitle())
			.content(finalContent)
			.remindCount(0)
			.build();

		Note savedNote = noteRepository.save(note);
		log.info("노트 생성 완료 - 노트 ID: {}, 사용자 ID: {}", savedNote.getId(), userId);

		// Elasticsearch에 인덱싱
		try {
			NoteDocument noteDocument = NoteDocument.from(savedNote);
			noteSearchService.indexNote(noteDocument);
			log.info("Elasticsearch 인덱싱 완료 - 노트 ID: {}", savedNote.getId());
		} catch (Exception e) {
			log.error("Elasticsearch 인덱싱 실패 - 노트 ID: {}", savedNote.getId(), e);
			// Elasticsearch 인덱싱 실패는 메인 로직에 영향 없음
		}

		return savedNote;
	}

	@Override
	public NoteResponse getNoteById(Long noteId, Long userId) {
		log.info("Getting note ID: {} for user ID: {}",noteId, userId);
		
		// 1. 노트 존재 여부 확인
		Note note = noteRepository.findById(noteId).orElseThrow(() -> new BaseException(BaseResponseStatus.NOTE_NOT_FOUND));
		
		// 2. 노트 소유자 확인 (권한 검증)
		if (!note.getUser().getId().equals(userId)) {
			log.warn("User {} tried to access note {} owned by user {}",
				userId, noteId, note.getUser().getId());
			throw new BaseException(BaseResponseStatus.NOTE_ACCESS_DENIED);
		}

		// 3. NoteResponse로 변환 후 반환
		log.info("Note found successfully - ID: {}, User ID: {}", noteId, userId);
		return NoteResponse.from(note);
	}

	/**
	 * 노트 요청 데이터 검증
	 * @param request 노트 생성 요청 DTO
	 * @throws BaseException 검증 실패 시
	 */
	private void validateNoteRequest(NoteRequest request) {
		// title 검증
		if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
			log.warn("Note title is empty");
			throw new BaseException(BaseResponseStatus.NOTE_TITLE_EMPTY);
		}
		if (request.getTitle().length() > 64) {
			log.warn("Note title too long: {} characters", request.getTitle().length());
			throw new BaseException(BaseResponseStatus.NOTE_TITLE_TOO_LONG);
		}

		// content 검증
		if (request.getContent() == null || request.getContent().trim().isEmpty()) {
			log.warn("Note content is empty");
			throw new BaseException(BaseResponseStatus.NOTE_CONTENT_EMPTY);
		}
		if (request.getContent().length() > 2048) {
			log.warn("Note content too long: {} characters", request.getContent().length());
			throw new BaseException(BaseResponseStatus.NOTE_CONTENT_TOO_LONG);
		}
	}

	/**
	 * 이미지 파일들을 S3에 업로드하고 마크다운 형식으로 content에 삽입
	 * 현재는 S3 연동 전이므로 placeholder 로직
	 *
	 * @param originalContent 원본 content
	 * @param images 업로드할 이미지 파일 목록
	 * @return 이미지 마크다운이 포함된 최종 content
	 */
	private String processImagesAndContent(String originalContent, List<MultipartFile> images) {
		if (images == null || images.isEmpty()) {
			return originalContent;
		}

		// TODO: S3 업로드 후 실제 URL로 변경 예정
		// 현재는 임시로 파일명만 사용
		List<String> imageMarkdowns = images.stream()
			.filter(file -> !file.isEmpty())
			.map(file -> {
				String filename = file.getOriginalFilename();
				// TODO: S3 업로드 로직 추가 예정
				// String imageUrl = s3UploadService.upload(file);
				String imageUrl = "https://placeholder-s3-url/" + filename; // placeholder

				// 마크다운 이미지 문법: ![alt text](url)
				return String.format("![%s](%s)", filename, imageUrl);
			})
			.collect(Collectors.toList());

		// 원본 content 뒤에 이미지 마크다운 추가
		if (imageMarkdowns.isEmpty()) {
			return originalContent;
		}

		StringBuilder contentBuilder = new StringBuilder(originalContent);
		contentBuilder.append("\n\n"); // 구분을 위한 줄바꿈

		for (String imageMarkdown : imageMarkdowns) {
			contentBuilder.append(imageMarkdown).append("\n");
		}

		return contentBuilder.toString().trim();
	}

	/**
	 * 최종 content 길이 검증
	 * 이미지 마크다운 추가 후 DB 제약조건(2048자)을 초과하는지 확인
	 *
	 * @param content 검증할 content
	 * @throws BaseException content가 2048자를 초과하는 경우
	 */
	private void validateContentLength(String content) {
		final int MAX_CONTENT_LENGTH = 2048;
		if (content != null && content.length() > MAX_CONTENT_LENGTH) {
			log.warn("Content length exceeds maximum: {} > {}", content.length(), MAX_CONTENT_LENGTH);
			throw new BaseException(BaseResponseStatus.BAD_REQUEST);
		}
	}
}
