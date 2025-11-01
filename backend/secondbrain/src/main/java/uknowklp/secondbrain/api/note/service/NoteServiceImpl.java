package uknowklp.secondbrain.api.note.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	@Override
	public Note createNote(Long userId, NoteRequest request) {
		log.info("Creating note for user ID: {}", userId);

		// 사용자 존재 확인
	User user = userService.findById(userId)
		.orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

		// 노트 생성
		Note note = Note.builder()
			.user(user)
			.title(request.getTitle())
			.content(request.getContent())
			.remindCount(0)
			.build();

		Note savedNote = noteRepository.save(note);
		log.info("Note created successfully - ID: {}, User ID: {}", savedNote.getId(), userId);

		return savedNote;
	}
}
