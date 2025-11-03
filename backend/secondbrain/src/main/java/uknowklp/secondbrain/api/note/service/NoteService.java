package uknowklp.secondbrain.api.note.service;

import uknowklp.secondbrain.api.note.domain.Note;
import uknowklp.secondbrain.api.note.dto.NoteRequest;

public interface NoteService {
	/**
	 * 새로운 노트 생성
	 *
	 * @param userId 사용자 ID
	 * @param request 노트 생성 요청 DTO
	 * @return 생성된 노트
	 */
	Note createNote(Long userId, NoteRequest request);
}
