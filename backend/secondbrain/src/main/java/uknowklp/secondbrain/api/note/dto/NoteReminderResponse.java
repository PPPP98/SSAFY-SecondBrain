package uknowklp.secondbrain.api.note.dto;

import java.util.List;

import lombok.Builder;

/**
 * 리마인더 노트 목록 조회 응답 DTO (페이징 정보 포함)
 * 무한스크롤을 위한 페이지네이션 지원
 */
@Builder
public record NoteReminderResponse(
	List<NoteReminderResult> results,
	long totalCount,
	int currentPage,
	int totalPages,
	int pageSize
) {
}
