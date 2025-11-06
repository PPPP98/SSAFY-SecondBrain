package uknowklp.secondbrain.api.note.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KnowledgeGraphEvent(
	@JsonProperty("event_type") String eventType,
	@JsonProperty("note_id") Long noteId,
	@JsonProperty("user_id") Long userId,
	@JsonProperty("title") String title,
	@JsonProperty("content") String content
) {
	// 생성 이벤트 팩토리 메서드
	public static KnowledgeGraphEvent created(Long noteId, Long userId, String title, String content) {
		return new KnowledgeGraphEvent("note.created", noteId, userId, title, content);
	}

	// 수정 이벤트 (title, content null 허용)
	public static KnowledgeGraphEvent updated(Long noteId, Long userId, String title, String content) {
		return new KnowledgeGraphEvent("note.updated", noteId, userId, title, content);
	}

	// 삭제 이벤트
	public static KnowledgeGraphEvent deleted(Long noteId, Long userId) {
		return new KnowledgeGraphEvent("note.deleted", noteId, userId, null, null);
	}
}
