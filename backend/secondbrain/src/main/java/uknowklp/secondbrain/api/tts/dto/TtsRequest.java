package uknowklp.secondbrain.api.tts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TtsRequest(
	@NotBlank(message = "텍스트 입력은 필수")
	@Size(max = 2000, message = "텍스트는 최대 2000자까지 입력 가능합니다.")
	String text,
	// null인 경우 서비스 단에서 기본 값으로 처리
	String speaker
) {
}
