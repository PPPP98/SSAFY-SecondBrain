package uknowklp.secondbrain.api.note.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 노트 생성 요청 DTO
 * S3 연결 전까지는 JSON 형식으로 제목, 내용만 받음
 * TODO: S3 연결 후 multipart/form-data로 변경하여 이미지도 받을 예정
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRequest {

	@Schema(description = "노트 제목", example = "멀티 테스트", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "제목은 필수입니다.")
	@Size(max = 64, message = "제목은 최대 64자까지 입력 가능합니다.")
	private String title;

	@Schema(description = "노트 내용", example = "멀티 테스트 내용", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "내용은 필수입니다.")
	@Size(max = 2048, message = "내용은 최대 2048자까지 입력 가능합니다.")
	private String content;

	/**
	 * 노트에 첨부할 이미지 파일 목록 (optional)
	 * S3 업로드 전까지는 더미 URL로 마크다운 형식 삽입
	 */
	@Schema(description = "이미지 파일 목록 (optional)", type = "array", format = "binary")
	private List<MultipartFile> images;

	/**
	 * multipart/form-data 요청으로부터 NoteRequest 객체 생성
	 *
	 * @param title 노트 제목
	 * @param content 노트 내용
	 * @param images 이미지 파일 목록 (optional)
	 * @return 생성된 NoteRequest 객체
	 */
	public static NoteRequest of(String title, String content, List<MultipartFile> images) {
		return NoteRequest.builder()
			.title(title)
			.content(content)
			.images(images)
			.build();
	}
}
