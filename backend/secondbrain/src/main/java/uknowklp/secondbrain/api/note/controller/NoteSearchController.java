package uknowklp.secondbrain.api.note.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import uknowklp.secondbrain.api.note.domain.NoteDocument;
import uknowklp.secondbrain.api.note.dto.NoteSearchResponse;
import uknowklp.secondbrain.api.note.dto.NoteSearchResult;
import uknowklp.secondbrain.api.note.service.NoteSearchService;
import uknowklp.secondbrain.global.response.BaseResponse;
import uknowklp.secondbrain.global.security.jwt.dto.CustomUserDetails;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteSearchController {

	private final NoteSearchService noteSearchService;

	@GetMapping("/search")
	@Operation(summary = "노트 검색", description = "제목 + 내용 기반 검색, 유사한 노트도 검색됩니다.")
	public BaseResponse<NoteSearchResponse> searchNotes(
		@RequestParam String keyword,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		Long userId = userDetails.getUser().getId();
		Pageable pageable = PageRequest.of(page, size);
		Page<NoteDocument> searchResults = noteSearchService.searchByKeyword(keyword, userId, pageable);

		// NoteDocument -> NoteSearchResult 변환
		List<NoteSearchResult> results = searchResults.getContent().stream()
			.map(NoteSearchResult::from)
			.collect(Collectors.toList());

		NoteSearchResponse response = NoteSearchResponse.builder()
			.results(results)
			.totalCount(searchResults.getTotalElements())
			.currentPage(searchResults.getNumber())
			.totalPages(searchResults.getTotalPages())
			.pageSize(searchResults.getSize())
			.build();

		return new BaseResponse<>(response);
	}

	@GetMapping("/{noteId}/similar")
	@Operation(summary = "유사 노트 검색", description = "해당 노트와 유사한 5개 노트 검색")
	public BaseResponse<List<NoteSearchResult>> findSimilarNotes(
		@PathVariable Long noteId,
		@RequestParam(defaultValue = "5") int limit,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		Long userId = userDetails.getUser().getId();
		List<NoteDocument> similarNotes = noteSearchService.findSimilarNotes(noteId, userId, limit);

		List<NoteSearchResult> results = similarNotes.stream()
			.map(NoteSearchResult::from)
			.collect(Collectors.toList());

		return new BaseResponse<>(results);
	}
}
