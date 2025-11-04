package uknowklp.secondbrain.api.note.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import uknowklp.secondbrain.api.note.domain.Note;

@Getter
@Builder
public class NoteResponse {

    private Long noteId;
    private String title;
    private String content;
    private LocalDateTime remindAt;

    public static NoteResponse from(Note note) {
        return NoteResponse.builder()
            .noteId(note.getId())
            .title(note.getTitle())
            .content(note.getContent())
            .remindAt(note.getRemindAt())
            .build();
    }

}