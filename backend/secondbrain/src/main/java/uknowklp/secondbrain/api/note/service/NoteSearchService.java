package uknowklp.secondbrain.api.note.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import uknowklp.secondbrain.api.note.domain.NoteDocument;
import uknowklp.secondbrain.api.note.repository.NoteSearchRepository;

@Service
@RequiredArgsConstructor
public class NoteSearchService {

	private final NoteSearchRepository noteSearchRepository;

	public List<NoteDocument> searchNotes(String keyword) {
		return noteSearchRepository.findByTitleOrContent(keyword, keyword);
	}

	// todo: 임시로 데이터 저장하는 메서드 (테스트용)
	public void saveNote(NoteDocument note) {
		noteSearchRepository.save(note);
	}
}
