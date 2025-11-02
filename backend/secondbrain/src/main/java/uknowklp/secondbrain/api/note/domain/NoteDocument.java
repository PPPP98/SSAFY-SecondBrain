package uknowklp.secondbrain.api.note.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = "notes")
public class NoteDocument {

	@Id
	private Long id; // PostgreSQL의 note_id와 매핑

	@Field(type = FieldType.Text, analyzer = "nori_analyzer", searchAnalyzer = "nori_analyzer")
	private String title;

	@Field(type = FieldType.Text, analyzer = "nori_analyzer", searchAnalyzer = "nori_analyzer")
	private String content;

	@Field(type = FieldType.Keyword)
	private Long memberId; // PostgreSQL의 member_id와 매핑

	@Field(type = FieldType.Date)
	private LocalDateTime createdAt;

	@Field(type = FieldType.Date)
	private LocalDateTime updatedAt;

	@Field(type = FieldType.Date)
	private LocalDateTime remindAt; // 리마인드 예정 시간

	@Field(type = FieldType.Integer)
	private Integer remindCount; // 리마인드 횟수

	@Builder
	public NoteDocument(Long id, String title, String content, Long memberId,
		LocalDateTime createdAt, LocalDateTime updatedAt,
		LocalDateTime remindAt, Integer remindCount) {
		this.id = id;
		this.title = title;
		this.content = content;
		this.memberId = memberId;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.remindAt = remindAt;
		this.remindCount = remindCount != null ? remindCount : 0;
	}
}
