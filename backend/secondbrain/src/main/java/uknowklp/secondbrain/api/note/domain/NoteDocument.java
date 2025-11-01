package uknowklp.secondbrain.api.note.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Document(indexName = "notes")
public class NoteDocument {

	@Id
	private String id;

	@Field(type = FieldType.Text, analyzer = "nori")
	private String title;

	@Field(type = FieldType.Text, analyzer = "nori")
	private String content;

	@Builder
	public NoteDocument(String id, String title, String content) {
		this.id = id;
		this.title = title;
		this.content = content;
	}
}
