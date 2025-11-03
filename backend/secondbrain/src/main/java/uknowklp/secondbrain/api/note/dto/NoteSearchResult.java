package uknowklp.secondbrain.api.note.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import uknowklp.secondbrain.api.note.domain.NoteDocument;

// 검색 결과 DTO
@Builder
public record NoteSearchResult(
	Long id,
	String title,
	String content,
	Long userId,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	Integer remindCount
) {
	// NoteDocument를 NoteSearchResult로 변환
	public static NoteSearchResult from(NoteDocument document) {
		return NoteSearchResult.builder()
			.id(document.getId())
			.title(document.getTitle())
			.content(document.getContent())
			.userId(document.getUserId())
			.createdAt(document.getCreatedAt())
			.updatedAt(document.getUpdatedAt())
			.remindCount(document.getRemindCount())
			.build();
	}
}
