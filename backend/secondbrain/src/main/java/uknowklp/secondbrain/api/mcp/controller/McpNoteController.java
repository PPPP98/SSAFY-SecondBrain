package uknowklp.secondbrain.api.mcp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import uknowklp.secondbrain.api.mcp.service.McpNoteService;
import uknowklp.secondbrain.api.note.dto.NoteRequest;
import uknowklp.secondbrain.api.note.dto.NoteResponse;
import uknowklp.secondbrain.global.response.BaseResponse;

// MCP용 Note 컨트롤러 (API Key 기반 인증)
@RestController
@RequestMapping("/api/mcp/notes")
@RequiredArgsConstructor
public class McpNoteController {

	private final McpNoteService mcpNoteService;

	// 노트 생성 (API Key 인증)
	@PostMapping
	@Operation(summary = "MCP - 노트 생성", description = "X-API-Key 헤더로 인증하여 노트를 생성합니다")
	public ResponseEntity<BaseResponse<NoteResponse>> createNote(
		@RequestHeader("X-API-Key") String apiKey,
		@RequestBody NoteRequest request
	) {
		NoteResponse response = mcpNoteService.createNote(apiKey, request);
		return ResponseEntity.ok(new BaseResponse<>(response));
	}

	// 노트 조회 (API Key 인증)
	@GetMapping("/{noteId}")
	@Operation(summary = "MCP - 노트 조회", description = "X-API-Key 헤더로 인증하여 노트를 조회합니다")
	public ResponseEntity<BaseResponse<NoteResponse>> getNote(
		@RequestHeader("X-API-Key") String apiKey,
		@PathVariable Long noteId
	) {
		NoteResponse response = mcpNoteService.getNote(apiKey, noteId);
		return ResponseEntity.ok(new BaseResponse<>(response));
	}
}
