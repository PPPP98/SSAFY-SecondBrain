package uknowklp.secondbrain.api.note.dto;

import java.time.LocalDateTime;

public record ReminderMessage (
	Long noteId,
	Long userId,
	String title,
	String question,
	LocalDateTime scheduledTime,
	Integer attemptCount
){
}
