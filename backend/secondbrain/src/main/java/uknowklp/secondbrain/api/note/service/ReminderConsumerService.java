package uknowklp.secondbrain.api.note.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.note.dto.ReminderMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderConsumerService {

	private final NoteService noteService;

	// RabbitMQ Queue에서 메시지 수신
	@RabbitListener(queues = "reminder.queue")
	public void handleReminder(ReminderMessage message) {
		try {
			// todo: 개발 완료 후 삭제할 로그
			log.info("리마인더 메시지 수신 - 노트 ID: {}, 사용자 ID: {}, 리마인드 횟수: {}", message.noteId(), message.userId(),
				message.attemptCount());

			// Note 검증할 메서드 호출
			noteService.processReminder(message.noteId());
		} catch (Exception e) {
			log.error("리마인더 처리 실패한 노트 ID: {}, 에러 메시지: {}", message.noteId(), e.getMessage());
			// 예외 발생시 RabbitMQ가 재시도 정책에 따라 재전송하게 됨
			throw e;
		}
	}
}
