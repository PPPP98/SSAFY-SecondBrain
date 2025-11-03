package uknowklp.secondbrain.api.note.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 노트 생성 요청 DTO
 * multipart/form-data 형식으로 제목, 내용, 이미지 파일들을 받음
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRequest {

	@NotBlank(message = "제목은 필수입니다.")
	@Size(max = 64, message = "제목은 최대 64자까지 입력 가능합니다.")
	private String title;

	@NotBlank(message = "내용은 필수입니다.")
	@Size(max = 2048, message = "내용은 최대 2048자까지 입력 가능합니다.")
	private String content;

	/**
	 * 노트에 첨부할 이미지 파일 목록 (optional)
	 * S3 업로드 후 URL을 마크다운 형식으로 content에 삽입 예정
	 */
	private List<MultipartFile> images;
}
