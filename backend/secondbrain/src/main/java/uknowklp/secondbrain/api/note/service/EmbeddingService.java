package uknowklp.secondbrain.api.note.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.CreateEmbeddingResponse;
import com.openai.models.EmbeddingCreateParams;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

// GMS의 text-embedding-3-small 모델을 사용하여 검색어를 1536차원 벡터로 변환
@Slf4j
@Service
public class EmbeddingService {

	@Value("${gms.api-key}")
	private String apiKey;

	@Value("${gms.embedding-model}")
	private String embeddingModel;

	@Value("${gms.openai-base-url}")
	private String openaiBaseUrl;

	private OpenAIClient client;

	@PostConstruct
	public void init() {
		// FastAPI와 동일: OpenAI SDK 초기화 (base_url 설정)
		this.client = OpenAIOkHttpClient.builder()
			.apiKey(apiKey)
			.baseUrl(openaiBaseUrl)
			.build();

		log.debug("✅ OpenAI 클라이언트 초기화 (GMS): {}", openaiBaseUrl);
	}

	// 텍스트를 1536차원 임베딩 벡터로 변환 (OpenAI SDK 사용)
	public List<Double> generateEmbedding(String text) {
		try {
			log.debug("🤖 임베딩 생성 중 - 모델: {}, 텍스트 길이: {}자", embeddingModel, text.length());

			// FastAPI와 동일: client.embeddings.create()
			EmbeddingCreateParams params = EmbeddingCreateParams.builder()
				.model(embeddingModel)
				.input(EmbeddingCreateParams.Input.ofString(text))
				.encodingFormat(EmbeddingCreateParams.EncodingFormat.FLOAT)
				.build();

			CreateEmbeddingResponse response = client.embeddings().create(params);

			// 임베딩 벡터 추출
			List<Double> embedding = response.data().get(0).embedding();

			log.debug("✅ 임베딩 생성 완료 - 차원: {}", embedding.size());

			return embedding;

		} catch (Exception e) {
			log.error("❌ 임베딩 생성 실패: {}", e.getMessage(), e);
			throw new RuntimeException("임베딩 생성 중 오류 발생", e);
		}
	}
}
