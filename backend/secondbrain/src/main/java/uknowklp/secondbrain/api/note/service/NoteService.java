package uknowklp.secondbrain.api.note.service;

import java.util.List;

import uknowklp.secondbrain.api.note.domain.Note;
import uknowklp.secondbrain.api.note.dto.NoteRequest;
import uknowklp.secondbrain.api.note.dto.NoteResponse;

public interface NoteService {
	/**
	 * 새로운 노트 생성
	 *
	 * @param userId 사용자 ID
	 * @param request 노트 생성 요청 DTO
	 * @return 생성된 노트
	 */
	Note createNote(Long userId, NoteRequest request);

	/**
	 * 노트 조회
	 *
	 * @param noteId 조회할 노트 ID
	 * @param userId 사용자 ID (권한 검증용)
	 * @return 조회된 노트 정보
	 */
	NoteResponse getNoteById(Long noteId, Long userId);

	/**
	 * 노트 수정
	 *
	 * @param noteId 수정할 노트 ID
	 * @param userId 사용자 ID (권한 검증용)
	 * @param request 노트 수정 요청 DTO
	 * @return 수정된 노트 정보
	 */
	NoteResponse updateNote(Long noteId, Long userId, NoteRequest request);

	/**
	 * 노트 삭제 (다중 삭제 지원)
	 *
	 * @param noteIds 삭제할 노트 ID 목록
	 * @param userId 사용자 ID (권한 검증용)
	 */
	void deleteNotes(List<Long> noteIds, Long userId);

	// 특정 노트의 리마인더 활성화
	Note enableNoteReminder(Long noteId, Long userId);

	// 특정 노트의 리마인더 비활성화
	Note disableNoteReminder(Long noteId, Long userId);

	// 리마인더 처리 및 다음 리마인더 스케줄링
	void processReminder(Long noteId);
}
