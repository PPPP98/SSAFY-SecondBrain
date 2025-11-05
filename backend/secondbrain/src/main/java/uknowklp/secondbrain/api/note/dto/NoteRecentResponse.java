package uknowklp.secondbrain.api.note.dto;

import uknowklp.secondbrain.api.note.domain.Note;

public record NoteRecentResponse (
	Long noteId,
	String title
){
	public static NoteRecentResponse from(Note note){
		return new NoteRecentResponse(note.getId(), note.getTitle());
	}
}
