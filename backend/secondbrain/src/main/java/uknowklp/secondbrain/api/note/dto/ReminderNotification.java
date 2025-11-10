package uknowklp.secondbrain.api.note.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;

// WebSocket으로 전송할 리마인더 알림 DTO
@Builder
public record ReminderNotification(
	Long noteId,
	String title,
	String question, // GMS 생성 질문
	Integer remindCount, // 현재 리마인더 횟수

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime timestamp // 발송 시간
) {
}
