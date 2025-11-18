package uknowklp.secondbrain.api.mcp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import uknowklp.secondbrain.api.apikey.service.ApiKeyService;
import uknowklp.secondbrain.api.note.domain.Note;
import uknowklp.secondbrain.api.note.dto.NoteRequest;
import uknowklp.secondbrain.api.note.dto.NoteResponse;
import uknowklp.secondbrain.api.note.service.NoteService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class McpNoteServiceImpl implements McpNoteService {

	private final ApiKeyService apiKeyService;
	private final NoteService noteService;

	// API Key로 노트 생성
	@Override
	@Transactional
	public NoteResponse createNote(String apiKey, NoteRequest request) {
		// API Key 검증 및 userId 추출
		Long userId = apiKeyService.validateApiKey(apiKey).userId();

		// 기존 NoteService를 사용하여 노트 생성
		Note note = noteService.createNote(userId, request);

		// NoteResponse로 변환하여 반환
		return noteService.getNoteById(note.getId(), userId);
	}

	// API Key로 노트 조회
	@Override
	public NoteResponse getNote(String apiKey, Long noteId) {
		// API Key 검증 및 userId 추출
		Long userId = apiKeyService.validateApiKey(apiKey).userId();

		// 기존 NoteService를 사용하여 노트 조회 (권한 검증 포함)
		return noteService.getNoteById(noteId, userId);
	}
}
