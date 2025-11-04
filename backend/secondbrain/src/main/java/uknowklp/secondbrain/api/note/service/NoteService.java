package uknowklp.secondbrain.api.note.service;

import java.util.List;

import uknowklp.secondbrain.api.note.domain.Note;
import uknowklp.secondbrain.api.note.dto.NoteRecentResponse;
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

	/**
	 * 최근 노트 목록 조회 (상위 10개)
	 * updatedAt 기준 내림차순, 동일 시 noteId 기준 내림차순 정렬
	 *
	 * @param userId 사용자 ID
	 * @return 최근 노트 목록 (최대 10개), 데이터 없으면 null
	 */
	List<NoteRecentResponse> getRecentNotes(Long userId);
}
