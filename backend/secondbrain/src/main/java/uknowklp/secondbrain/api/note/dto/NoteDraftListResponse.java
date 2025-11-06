package uknowklp.secondbrain.api.note.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Draft 목록 조회 응답 DTO
 *
 * 사용 시나리오:
 * - 브라우저 재시작 후 미저장 Draft 복구
 * - 여러 노트를 동시에 작성 중인 경우
 */
@Builder
@Getter
@AllArgsConstructor
public class NoteDraftListResponse {

	/** Draft 목록 */
	private List<NoteDraftResponse> drafts;

	/** 전체 개수 */
	private Integer totalCount;
}
