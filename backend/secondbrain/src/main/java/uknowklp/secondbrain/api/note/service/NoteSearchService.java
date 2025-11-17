package uknowklp.secondbrain.api.note.service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
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
import uknowklp.secondbrain.api.note.dto.VectorSearchResult;
import uknowklp.secondbrain.api.note.repository.NoteSearchRepository;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponseStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteSearchService {

	private final NoteSearchRepository noteSearchRepository;
	private final ElasticsearchOperations elasticsearchOperations;
	private final VectorSearchService vectorSearchService;
	private final EmbeddingService embeddingService;

	// 하이브리드 검색: Elasticsearch BM25 (70%) + Neo4j 벡터 유사도 (30%)
	public Page<NoteDocument> searchByKeyword(String keyword, Long userId, Pageable pageable) {
		// 키워드 검증
		if (keyword == null || keyword.trim().isEmpty()) {
			throw new BaseException(BaseResponseStatus.INVALID_SEARCH_KEYWORD);
		}

		try {
			// 1. Elasticsearch 검색 (항상 실행)
			CompletableFuture<List<NoteDocument>> elasticFuture = CompletableFuture.supplyAsync(() ->
				searchByElasticsearch(keyword, userId, 100)
			);

			// 2. 임베딩 벡터 생성 및 Neo4j 검색 (실패 시 빈 리스트 반환)
			CompletableFuture<List<VectorSearchResult>> vectorFuture = CompletableFuture.supplyAsync(() -> {
				try {
					List<Double> queryEmbedding = embeddingService.generateEmbedding(keyword);
					return vectorSearchService.searchSimilarNotes(userId, queryEmbedding, 100);
				} catch (Exception e) {
					log.warn("벡터 검색 실패, Elasticsearch만 사용 - 키워드: {}, 오류: {}", keyword, e.getMessage());
					return Collections.emptyList();
				}
			});

			// 3. 두 검색 결과 대기
			List<NoteDocument> elasticResults = elasticFuture.join();
			List<VectorSearchResult> vectorResults = vectorFuture.join();

			// 4. RRF로 검색 결과 병합 및 재정렬 (벡터 결과가 없으면 Elasticsearch만 반환)
			List<NoteDocument> mergedResults = vectorResults.isEmpty()
				? elasticResults
				: mergeAndRerank(elasticResults, vectorResults);

			// 5. 페이징 적용
			int start = (int) pageable.getOffset();
			int end = Math.min(start + pageable.getPageSize(), mergedResults.size());
			List<NoteDocument> pagedResults = mergedResults.subList(start, end);

			if (vectorResults.isEmpty()) {
				log.info("Elasticsearch 전용 검색 완료 - 키워드: '{}', 결과: {}건",
					keyword, elasticResults.size());
			} else {
				log.info("하이브리드 검색 완료 - 키워드: '{}', Elastic: {}건, Vector: {}건, 최종: {}건",
					keyword, elasticResults.size(), vectorResults.size(), mergedResults.size());
			}

			return new PageImpl<>(pagedResults, pageable, mergedResults.size());
		} catch (BaseException e) {
			throw e;
		} catch (Exception e) {
			log.error("하이브리드 검색 오류 - 키워드: {}, 예외: {}", keyword, e.getMessage(), e);
			throw new BaseException(BaseResponseStatus.ELASTICSEARCH_ERROR);
		}
	}

	// Elasticsearch 전용 검색 (BM25 키워드 매칭)
	private List<NoteDocument> searchByElasticsearch(String keyword, Long userId, int limit) {
		try {
			// Multi-match 쿼리 생성 (제목 5배 가중치)
			Query multiMatchQuery = MultiMatchQuery.of(m -> m
				.query(keyword)
				.fields("title^5", "content")
				.type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
				.fuzziness("AUTO")
				.prefixLength(1)
				.maxExpansions(30)
			)._toQuery();

			// Bool 쿼리 생성
			BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder()
				.must(multiMatchQuery);

			// userId 필터 추가
			if (userId != null) {
				Query userFilter = TermQuery.of(t -> t
					.field("userId")
					.value(userId)
				)._toQuery();
				boolQueryBuilder.filter(userFilter);
			}

			// Native Query 생성 (최소 점수 5 이상만 반환)
			NativeQuery searchQuery = NativeQuery.builder()
				.withQuery(boolQueryBuilder.build()._toQuery())
				.withMinScore(5.0f)
				.withMaxResults(limit)
				.build();

			// 검색 실행
			SearchHits<NoteDocument> searchHits = elasticsearchOperations.search(
				searchQuery,
				NoteDocument.class
			);

			// 결과 변환
			return searchHits.getSearchHits().stream()
				.map(SearchHit::getContent)
				.toList();
		} catch (Exception e) {
			log.error("Elasticsearch 검색 실패 - 키워드: {}", keyword, e);
			return Collections.emptyList();
		}
	}

	// RRF (Reciprocal Rank Fusion) 알고리즘으로 검색 결과 병합
	private List<NoteDocument> mergeAndRerank(
		List<NoteDocument> elasticResults,
		List<VectorSearchResult> vectorResults
	) {
		final int k = 60; // RRF 상수
		Map<Long, Double> scoreMap = new HashMap<>();
		Map<Long, NoteDocument> documentMap = new HashMap<>();

		// Elasticsearch 결과 점수 계산 (가중치 70%)
		for (int i = 0; i < elasticResults.size(); i++) {
			NoteDocument doc = elasticResults.get(i);
			Long noteId = doc.getId();
			double rrfScore = 0.7 / (k + i + 1); // rank는 0부터 시작하므로 +1
			scoreMap.put(noteId, scoreMap.getOrDefault(noteId, 0.0) + rrfScore);
			documentMap.put(noteId, doc);
		}

		// Vector 검색 결과 점수 계산 (가중치 30%)
		for (int i = 0; i < vectorResults.size(); i++) {
			VectorSearchResult result = vectorResults.get(i);
			Long noteId = result.noteId();
			double rrfScore = 0.3 / (k + i + 1);
			scoreMap.put(noteId, scoreMap.getOrDefault(noteId, 0.0) + rrfScore);

			// Vector 결과에만 있는 문서는 Elasticsearch에서 조회
			if (!documentMap.containsKey(noteId)) {
				noteSearchRepository.findById(noteId.toString())
					.ifPresent(doc -> documentMap.put(noteId, doc));
			}
		}

		// 최종 점수 기준으로 정렬
		return scoreMap.entrySet().stream()
			.sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
			.map(entry -> documentMap.get(entry.getKey()))
			.filter(Objects::nonNull)
			.toList();
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
				.fields("title^5", "content")       // 제목 가중치 5배 (키워드 검색과 동일)
				.type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
				.fuzziness("AUTO")                  // 단어 길이 기반 자동 조정
				.prefixLength(1)                    // 첫 글자 고정
				.maxExpansions(30)
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

			// Native Query 생성 (최소 점수 5 이상만 반환)
			NativeQuery searchQuery = NativeQuery.builder()
				.withQuery(boolQueryBuilder.build()._toQuery())
				.withMinScore(5.0f)                 // 점수 5 미만 결과 제외
				.withMaxResults(limit)
				.build();

			// 검색 실행
			SearchHits<NoteDocument> searchHits = elasticsearchOperations.search(
				searchQuery,
				NoteDocument.class
			);

			// 결과 반환 및 유사도 점수 로깅
			List<NoteDocument> results = searchHits.getSearchHits().stream()
				.peek(hit -> log.info("유사 노트 발견 - ID: {}, 제목: '{}', 유사도 점수: {}",
					hit.getContent().getId(),
					hit.getContent().getTitle(),
					String.format("%.2f", hit.getScore())))
				.map(SearchHit::getContent)
				.collect(Collectors.toList());

			log.info("유사 노트 검색 완료 - 기준 노트 ID: {}, {}건 발견", noteId, results.size());

			return results;
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

	// 노트 일괄 삭제
	public void bulkDeleteNotes(List<String> noteIds) {
		noteSearchRepository.deleteAllById(noteIds);
	}
}
