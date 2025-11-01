package uknowklp.secondbrain.api.note.repository;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import uknowklp.secondbrain.api.note.domain.NoteDocument;

public interface NoteSearchRepository extends ElasticsearchRepository<NoteDocument, String> {
	// 제목이나 내용에 키워드 포함된 노트 검색
	List<NoteDocument> findByTitleOrContent(String title, String content);
}
