package uknowklp.secondbrain.api.note.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 텍스트 임베딩 생성 서비스
// GMS의 text-embedding-3-small 모델을 사용하여 검색어를 1536차원 벡터로 변환
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

	@Value("${gms.api-key}")
	private String apiKey;

	@Value("${gms.embedding-model}")
	private String embeddingModel;

	@Value("${gms.base-url}")
	private String baseUrl;

	private final RestTemplate restTemplate;

	// 텍스트를 1536차원 임베딩 벡터로 변환
	public List<Double> generateEmbedding(String text) {
		try {
			// OpenAI API 요청 바디 구성
			Map<String, Object> requestBody = Map.of(
				"model", embeddingModel,
				"input", text
			);

			// HTTP 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(apiKey);

			// HTTP 요청 엔티티 생성
			HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

			// GMS API 호출 (OpenAI 호환 엔드포인트)
			String url = "https://gms.ssafy.io/gmsapi/api.openai.com/v1/embeddings";

			log.debug("임베딩 API 호출 - 모델: {}, 텍스트 길이: {}", embeddingModel, text.length());

			EmbeddingResponse response = restTemplate.postForObject(url, request, EmbeddingResponse.class);

			// 응답 검증
			if (response == null || response.data == null || response.data.isEmpty()) {
				throw new RuntimeException("임베딩 응답이 비어있음");
			}

			// 벡터 추출
			List<Double> embedding = response.data.get(0).embedding;

			log.debug("임베딩 생성 완료 - 차원: {}", embedding.size());

			return embedding;

		} catch (Exception e) {
			log.error("임베딩 생성 실패: {}", e.getMessage(), e);
			throw new RuntimeException("임베딩 생성 중 오류 발생", e);
		}
	}

	// GMS API 응답 구조 (OpenAI 호환)
	private static class EmbeddingResponse {
		public List<EmbeddingData> data;
	}

	private static class EmbeddingData {
		public List<Double> embedding;
	}
}
