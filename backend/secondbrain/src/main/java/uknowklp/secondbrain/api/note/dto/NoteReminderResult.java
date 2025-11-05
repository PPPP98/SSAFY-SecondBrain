package uknowklp.secondbrain.api.note.dto;

import lombok.Builder;
import uknowklp.secondbrain.api.note.domain.Note;

/**
 * 리마인더 노트 목록 조회 결과 DTO
 * noteId와 title만 포함
 */
@Builder
public record NoteReminderResult(
	Long noteId,
	String title
) {
	/**
	 * Note 엔티티를 NoteReminderResult로 변환
	 */
	public static NoteReminderResult from(Note note) {
		return NoteReminderResult.builder()
			.noteId(note.getId())
			.title(note.getTitle())
			.build();
	}
}
