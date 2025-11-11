package uknowklp.secondbrain.api.tts.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uknowklp.secondbrain.global.config.ClovaVoiceConfig;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponseStatus;

@Slf4j
@Service
public class TtsService {

	private final WebClient clovaWebClient;
	private final ClovaVoiceConfig config;

	// webClient(GMS) bean이 2개라서 명시를 위한 @Qualifier를 사용하기 위해 생성자 직접 작성
	public TtsService(@Qualifier("clovaVoiceWebClient") WebClient clovaWebClient,
		ClovaVoiceConfig config) {
		this.clovaWebClient = clovaWebClient;
		this.config = config;
	}

	public Mono<byte[]> convert(String text, String speaker) {
		// 텍스트 길이 검증 (Naver API 제한: 2000자)
		if (text.length() > 2000) {
			log.warn("TTS 텍스트 길이 초과: {}자", text.length());
			throw new BaseException(BaseResponseStatus.TTS_TEXT_TOO_LONG);
		}

		// speaker == null이면 config 기본 값 사용
		String voiceSpeaker = (speaker == null || speaker.isBlank())
			? config.getDefaultSpeaker()
			: speaker;

		// naver API 요청 파라미터
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("text", text);
		params.add("speaker", voiceSpeaker);
		params.add("volume", String.valueOf(config.getDefaultVolume()));
		params.add("speed", String.valueOf(config.getDefaultSpeed()));
		params.add("pitch", String.valueOf(config.getDefaultPitch()));
		params.add("format", config.getDefaultFormat());

		// 요청 파라미터 로그
		log.info("TTS 요청 파라미터: {}", params);

		// API 호출 및 바이너리 데이터로 반환
		return clovaWebClient.post()
			.body(BodyInserters.fromFormData(params))
			.retrieve()
			// 에러 응답 상세 로그
			.onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
				response -> response.bodyToMono(String.class)
					.doOnNext(errorBody -> log.error("Naver API 에러 응답: {}", errorBody))
					.flatMap(errorBody -> Mono.error(new BaseException(BaseResponseStatus.TTS_API_ERROR))))
			.bodyToMono(byte[].class)
			.doOnSuccess(bytes -> log.info("TTS 변환 성공, 텍스트 길이: {}자, 음성:{}, 크기:{}bytes",
				text.length(), voiceSpeaker, bytes.length))
			.doOnError(e -> log.error("TTS API 호출 실패", e));
	}
}
