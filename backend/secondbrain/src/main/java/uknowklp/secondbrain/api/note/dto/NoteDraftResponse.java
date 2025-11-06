package uknowklp.secondbrain.api.note.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uknowklp.secondbrain.api.note.domain.NoteDraft;

/**
 * Draft 조회 응답 DTO
 */
@Builder
@Getter
@AllArgsConstructor
public class NoteDraftResponse {

	private String noteId;
	private String title;
	private String content;
	private Long version;
	private LocalDateTime lastModified;

	/**
	 * NoteDraft -> NoteDraftResponse 변환
	 *
	 * @param draft Redis에서 조회한 NoteDraft
	 * @return NoteDraftResponse
	 */
	public static NoteDraftResponse from(NoteDraft draft) {
		return NoteDraftResponse.builder()
			.noteId(draft.getNoteId())
			.title(draft.getTitle())
			.content(draft.getContent())
			.version(draft.getVersion())
			.lastModified(draft.getLastModified())
			.build();
	}
}
