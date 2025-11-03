package uknowklp.secondbrain.api.note.dto;

import java.util.List;

import lombok.Builder;

// 노트 검색 응답 DTO (페이징 정보 포함)
@Builder
public record NoteSearchResponse(
	List<NoteSearchResult> results,
	long totalCount,
	int currentPage,
	int totalPages,
	int pageSize
) {
}
