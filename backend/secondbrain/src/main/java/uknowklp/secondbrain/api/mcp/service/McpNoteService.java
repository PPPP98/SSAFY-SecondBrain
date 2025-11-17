package uknowklp.secondbrain.api.mcp.service;

import uknowklp.secondbrain.api.note.dto.NoteRequest;
import uknowklp.secondbrain.api.note.dto.NoteResponse;

// MCP용 Note 서비스 (API Key 기반 인증)
public interface McpNoteService {

	// API Key로 노트 생성
	NoteResponse createNote(String apiKey, NoteRequest request);

	// API Key로 노트 조회
	NoteResponse getNote(String apiKey, Long noteId);
}
