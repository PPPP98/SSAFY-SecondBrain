package uknowklp.secondbrain.api.note.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 노트 삭제 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteDeleteRequest {

	@Schema(description = "삭제할 노트 ID 목록", example = "[1, 2, 3]", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotEmpty(message = "삭제할 노트 ID는 최소 1개 이상이어야 합니다.")
	private List<@NotNull Long> noteIds;
}
