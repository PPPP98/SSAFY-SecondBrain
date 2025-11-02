package uknowklp.secondbrain.api.note.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import uknowklp.secondbrain.api.note.domain.NoteDocument;

// Elasticsearch 기본 CRUD Repository
// 복잡한 검색 로직은 NoteSearchService에서 ElasticsearchOperations로 처리
public interface NoteSearchRepository extends ElasticsearchRepository<NoteDocument, String> {
}
