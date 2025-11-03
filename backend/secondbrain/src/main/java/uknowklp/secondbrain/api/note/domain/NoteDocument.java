package uknowklp.secondbrain.api.note.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = "notes")
@Setting(settingPath = "elasticsearch/note-index-settings.json")
public class NoteDocument {

	@Id
	private Long id; // note_id

	@Field(type = FieldType.Text, analyzer = "nori_analyzer", searchAnalyzer = "nori_analyzer")
	private String title;

	@Field(type = FieldType.Text, analyzer = "nori_analyzer", searchAnalyzer = "nori_analyzer")
	private String content;

	@Field(type = FieldType.Keyword)
	private Long userId; // user_id (User의 ID)

	@Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
	private LocalDateTime createdAt;

	@Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
	private LocalDateTime updatedAt;

	@Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
	private LocalDateTime remindAt;

	@Field(type = FieldType.Integer)
	private Integer remindCount;

	@Builder
	public NoteDocument(Long id, String title, String content, Long userId,
		LocalDateTime createdAt, LocalDateTime updatedAt,
		LocalDateTime remindAt, Integer remindCount) {
		this.id = id;
		this.title = title;
		this.content = content;
		this.userId = userId;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.remindAt = remindAt;
		this.remindCount = remindCount != null ? remindCount : 0;
	}

	// Note 엔티티로부터 NoteDocument 생성
	public static NoteDocument from(Note note) {
		return NoteDocument.builder()
			.id(note.getId())
			.title(note.getTitle())
			.content(note.getContent())
			.userId(note.getUser().getId())
			.createdAt(note.getCreatedAt())
			.updatedAt(note.getUpdatedAt())
			.remindAt(note.getRemindAt())
			.remindCount(note.getRemindCount())
			.build();
	}
}
