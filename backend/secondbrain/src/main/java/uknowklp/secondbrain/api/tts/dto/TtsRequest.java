package uknowklp.secondbrain.api.tts.dto;

import jakarta.validation.constraints.NotBlank;

public record TtsRequest(
	@NotBlank(message = "텍스트 입력은 필수")
	String text,
	// null인 경우 서비스 단에서 기본 값으로 처리
	String speaker
) {
}
