package uknowklp.secondbrain.api.note.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.note.domain.Note;
import uknowklp.secondbrain.api.note.dto.NoteRequest;
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
	// TODO: S3 업로드 서비스 추가 예정
	// private final S3UploadService s3UploadService;

	@Override
	public Note createNote(Long userId, NoteRequest request) {
		log.info("Creating note for user ID: {}", userId);

		// 사용자 존재 확인
		User user = userService.findById(userId)
			.orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

		// 이미지 파일 처리 및 마크다운 content 생성
		String finalContent = processImagesAndContent(request.getContent(), request.getImages());

		// 서비스 레벨 검증: 최종 content 길이 확인
		validateContentLength(finalContent);

		// 노트 생성
		Note note = Note.builder()
			.user(user)
			.title(request.getTitle())
			.content(finalContent)
			.remindCount(0)
			.build();

		Note savedNote = noteRepository.save(note);
		log.info("Note created successfully - ID: {}, User ID: {}", savedNote.getId(), userId);

		return savedNote;
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
