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
@RequiredArgsConstructor
public class TtsService {

	// webclient bean이 두 개이기 때문에 명시해줌
	@Qualifier("clovaVoiceWebClient")
	private final WebClient clovaWebClient;
	private final ClovaVoiceConfig config;

	public Mono<byte[]> convert(String text, String speaker) {
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

		// API 호출 및 바이너리 데이터로 반환
		return clovaWebClient.post()
			.body(BodyInserters.fromFormData(params))
			.retrieve()
			.bodyToMono(byte[].class)
			.doOnSuccess(bytes -> log.info("TTS 변환 성공, 텍스트 길이: {}자, 음성:{}, 크기:{}bytes",
				text.length(), voiceSpeaker, bytes.length))
			.doOnError(e -> log.error("TTS API 호출 실패, e"))
			.onErrorMap(e -> new BaseException(BaseResponseStatus.TTS_API_ERROR));
	}
}
