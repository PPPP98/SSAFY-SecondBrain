package uknowklp.secondbrain.api.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 노트 생성/수정 요청 DTO (JSON)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRequest {

	@Schema(description = "노트 제목", example = "오늘의 학습 내용", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "제목은 필수입니다.")
	@Size(max = 64, message = "제목은 최대 64자까지 입력 가능합니다.")
	private String title;

	@Schema(description = "노트 내용 (TEXT 타입, 무제한)", example = "Spring Boot TTS 구현 완료", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "내용은 필수입니다.")
	private String content;
}
