package uknowklp.secondbrain.api.note.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.convert.MappingConversionException;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.note.domain.NoteDocument;
import uknowklp.secondbrain.api.note.repository.NoteSearchRepository;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponseStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteSearchService {

	private final NoteSearchRepository noteSearchRepository;
	private final ElasticsearchOperations elasticsearchOperations;

	// 키워드로 노트 검색 (제목 + 내용, 유사도 기반)
	public Page<NoteDocument> searchByKeyword(String keyword, Long userId, Pageable pageable) {
		// 키워드 검증
		if (keyword == null || keyword.trim().isEmpty()) {
			throw new BaseException(BaseResponseStatus.INVALID_SEARCH_KEYWORD);
		}

		try {
			// 1. Multi-match 쿼리 생성 (제목에 2배 가중치)
			Query multiMatchQuery = MultiMatchQuery.of(m -> m
				.query(keyword)
				.fields("title^2", "content")
				.type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
			)._toQuery();

			// 2. Bool 쿼리 생성
			BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder()
				.must(multiMatchQuery);

			// 3. userId 필터 추가 (특정 사용자의 노트만 검색)
			if (userId != null) {
				Query userFilter = TermQuery.of(t -> t
					.field("userId")
					.value(userId)
				)._toQuery();
				boolQueryBuilder.filter(userFilter);
			}

			// 4. Native Query 생성
			NativeQuery searchQuery = NativeQuery.builder()
				.withQuery(boolQueryBuilder.build()._toQuery())
				.withPageable(pageable)
				.build();

			// 5. 검색 실행
			SearchHits<NoteDocument> searchHits = elasticsearchOperations.search(
				searchQuery,
				NoteDocument.class
			);

			// 6. 결과 변환
			List<NoteDocument> content = searchHits.getSearchHits().stream()
				.map(SearchHit::getContent)
				.collect(Collectors.toList());

			return new PageImpl<>(content, pageable, searchHits.getTotalHits());
		} catch (BaseException e) {
			throw e;
		} catch (ElasticsearchException e) {
			log.error("Elasticsearch 서버 오류 - 키워드: {}, 오류: {}", keyword, e.getMessage(), e);
			throw new BaseException(BaseResponseStatus.ELASTICSEARCH_CONNECTION_ERROR);
		} catch (UncategorizedElasticsearchException e) {
			if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
				log.error("Elasticsearch 인덱스 없음 - 키워드: {}", keyword, e);
				throw new BaseException(BaseResponseStatus.ELASTICSEARCH_INDEX_NOT_FOUND);
			}
			log.error("Elasticsearch 연결 오류 - 키워드: {}", keyword, e);
			throw new BaseException(BaseResponseStatus.ELASTICSEARCH_CONNECTION_ERROR);
		} catch (MappingConversionException e) {
			log.error("Elasticsearch 매핑 오류 - 키워드: {}, 필드 변환 실패", keyword, e);
			throw new BaseException(BaseResponseStatus.ELASTICSEARCH_MAPPING_ERROR);
		} catch (Exception e) {
			log.error("예상치 못한 검색 오류 - 키워드: {}, 예외 타입: {}", keyword, e.getClass().getSimpleName(), e);
			throw new BaseException(BaseResponseStatus.ELASTICSEARCH_ERROR);
		}
	}

	// 특정 노트와 유사한 노트 찾기 (연관 높은 노트 추천)
	public List<NoteDocument> findSimilarNotes(Long noteId, Long userId, int limit) {
		try {
			// 기준 노트 조회
			NoteDocument baseNote = noteSearchRepository.findById(noteId.toString())
				.orElseThrow(() -> new BaseException(BaseResponseStatus.NOTE_NOT_FOUND));

			// 기준 노트의 제목과 내용으로 유사 노트 검색
			String searchText = baseNote.getTitle() + " " + baseNote.getContent();

			Query multiMatchQuery = MultiMatchQuery.of(m -> m
				.query(searchText)
				.fields("title^2", "content")
				.type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
			)._toQuery();

			// Bool 쿼리 생성 (자기 자신 제외 + userId 필터)
			BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder()
				.must(multiMatchQuery)
				.mustNot(TermQuery.of(t -> t
					.field("id")
					.value(noteId)
				)._toQuery());

			// userId 필터 추가
			if (userId != null) {
				boolQueryBuilder.filter(TermQuery.of(t -> t
					.field("userId")
					.value(userId)
				)._toQuery());
			}

			// Native Query 생성
			NativeQuery searchQuery = NativeQuery.builder()
				.withQuery(boolQueryBuilder.build()._toQuery())
				.withMaxResults(limit)
				.build();

			// 검색 실행
			SearchHits<NoteDocument> searchHits = elasticsearchOperations.search(
				searchQuery,
				NoteDocument.class
			);

			// 결과 반환
			return searchHits.getSearchHits().stream()
				.map(SearchHit::getContent)
				.collect(Collectors.toList());
		} catch (BaseException e) {
			throw e;
		} catch (ElasticsearchException e) {
			log.error("Elasticsearch 서버 오류 - 노트 ID: {}, 오류: {}", noteId, e.getMessage(), e);
			throw new BaseException(BaseResponseStatus.ELASTICSEARCH_CONNECTION_ERROR);
		} catch (UncategorizedElasticsearchException e) {
			if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
				log.error("Elasticsearch 인덱스 없음 - 노트 ID: {}", noteId, e);
				throw new BaseException(BaseResponseStatus.ELASTICSEARCH_INDEX_NOT_FOUND);
			}
			log.error("Elasticsearch 연결 오류 - 노트 ID: {}", noteId, e);
			throw new BaseException(BaseResponseStatus.ELASTICSEARCH_CONNECTION_ERROR);
		} catch (MappingConversionException e) {
			log.error("Elasticsearch 매핑 오류 - 노트 ID: {}, 필드 변환 실패", noteId, e);
			throw new BaseException(BaseResponseStatus.ELASTICSEARCH_MAPPING_ERROR);
		} catch (Exception e) {
			log.error("예상치 못한 유사 노트 검색 오류 - 노트 ID: {}, 예외 타입: {}", noteId, e.getClass().getSimpleName(), e);
			throw new BaseException(BaseResponseStatus.ELASTICSEARCH_ERROR);
		}
	}

	// 노트 인덱싱 (Note 저장/수정 시 호출)
	public void indexNote(NoteDocument noteDocument) {
		noteSearchRepository.save(noteDocument);
	}

	// 노트 삭제 (Note 삭제 시 호출)
	public void deleteNote(Long noteId) {
		noteSearchRepository.deleteById(noteId.toString());
	}
}
