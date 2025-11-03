package uknowklp.secondbrain.api.note.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

// 노트 검색 요청 DTO
@Builder
public record NoteSearchRequest(
	@NotBlank(message = "검색 키워드는 필수입니다")
	String keyword,

	@Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
	Integer page,

	@Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
	Integer size
) {
	// 기본값 설정
	public NoteSearchRequest {
		if (page == null) {
			page = 0;
		}
		if (size == null) {
			size = 10;
		}
	}
}
