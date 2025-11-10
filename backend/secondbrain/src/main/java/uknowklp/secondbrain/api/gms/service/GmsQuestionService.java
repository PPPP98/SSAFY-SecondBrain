package uknowklp.secondbrain.api.gms.service;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import uknowklp.secondbrain.api.gms.dto.GmsMessage;
import uknowklp.secondbrain.api.gms.dto.GmsRequest;
import uknowklp.secondbrain.api.gms.dto.GmsResponse;
import uknowklp.secondbrain.api.note.domain.Note;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmsQuestionService {

	private final WebClient gmsWebClient;

	@Value("${gms.model}")
	private String model;

	@Value("${gms.max-tokens}")
	private Integer maxTokens;

	@Value("${gms.temperature}")
	private Double temperature;

	// 리마인더 질문 비동기로 생성
	public Mono<String> makeReminderQuestion(Note note) {
		String prompt = createReminderPrompt(note);

		GmsRequest request = new GmsRequest(
			model,
			maxTokens,
			temperature,
			List.of(GmsMessage.user(prompt))
		);

		return gmsWebClient.post()
			.uri("/messages")
			.bodyValue(request)
			.retrieve()
			.bodyToMono(GmsResponse.class)
			.map(GmsResponse::extractText)
			.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
				.maxBackoff(Duration.ofSeconds(5))
				.doBeforeRetry(signal ->
					log.warn("GMS API 재시도 - noteId: {}, 시도: {}/3",
						note.getId(), signal.totalRetries() + 1)
				)
			)
			.onErrorResume(e -> {
				log.error("GMS API 실패 (비상용 질문 사용) - noteId: {}", note.getId(), e);
				return Mono.just(createFallbackQuestion(note));
			})
			.doOnSuccess(question ->
				log.debug("GMS 질문 생성 성공 - noteId: {}, 길이: {}",
					note.getId(), question.length())
			);
	}

	// 리마인더 프롬프트
	private String createReminderPrompt(Note note) {
		return String.format("""
				당신은 학습 효과를 극대화하는 교육 전문가입니다.
				사용자가 작성한 다음 노트를 복습할 수 있도록 핵심을 파악하고 복습 질문을 생성해주세요.
				
				노트 제목: %s
				노트 내용:
				%s
				
				요구사항:
				1. 노트의 핵심 개념을 파악하세요
				2. 사용자가 해당 내용을 제대로 이해했는지 확인할 수 있는 질문 1개를 만드세요
				3. 질문은 간결하고 명확하게 작성하세요 (50자 이내)
				4. 단순 암기가 아닌 이해도를 확인하는 질문으로 만드세요
				
				질문만 출력하고 다른 설명은 포함하지 마세요.
				""",
			note.getTitle(),
			note.getContent()
		);
	}

	// GMS 실패 시 진행할 비상용 질문
	private String createFallbackQuestion(Note note) {
		return String.format("'%s' 노트의 핵심 내용을 기억하시나요?", note.getTitle());
	}
}