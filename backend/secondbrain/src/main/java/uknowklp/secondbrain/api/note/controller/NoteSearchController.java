package uknowklp.secondbrain.api.note.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import uknowklp.secondbrain.api.note.domain.NoteDocument;
import uknowklp.secondbrain.api.note.repository.NoteSearchRepository;
import uknowklp.secondbrain.api.note.service.NoteSearchService;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class NoteSearchController {

	private final NoteSearchService noteSearchService;

	@GetMapping("/notes")
	public ResponseEntity<List<NoteDocument>> searchNotes(@RequestParam String keyword) {
		List<NoteDocument> results = noteSearchService.searchNotes(keyword);
		return ResponseEntity.ok(results);
	}

	// todo: 임시로 데이터 저장하는 API (테스트용)
	@PostMapping("/notes")
	public ResponseEntity<String> saveNote(@RequestBody NoteDocument note) {
		noteSearchService.saveNote(note);
		return ResponseEntity.ok("Note saved" + note.getId());
	}
}
