package uknowklp.secondbrain.api.note.service;

import java.time.LocalDateTime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.note.domain.Note;
import uknowklp.secondbrain.api.note.dto.ReminderNotification;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderNotificationService {

	private final SimpMessagingTemplate messagingTemplate;

	// 리마인더 알림 전송
	public void sendReminder(Note note, String question, int currentRemindCount) {
		Long userId = note.getUser().getId();

		// ReminderNotification DTO 생성
		ReminderNotification notification = ReminderNotification.builder()
			.noteId(note.getId())
			.title(note.getTitle())
			.question(question)
			.remindCount(currentRemindCount + 1) // 파라미터로 받은 currentCount + 1
			.timestamp(LocalDateTime.now())
			.build();

		// WebSocket으로 전송 (/topic/reminder/{userId})
		String destination = "/topic/reminder/" + userId;
		messagingTemplate.convertAndSend(destination, notification);

		log.info("WebSocket 알림 전송 완료 - userId: {}, noteId: {}, remindCount: {}/3, destination: {}",
			userId, note.getId(), currentRemindCount + 1, destination);
	}
}
