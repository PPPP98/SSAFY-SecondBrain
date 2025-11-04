package uknowklp.secondbrain.api.note.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.note.domain.Note;
import uknowklp.secondbrain.api.note.dto.ReminderMessage;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponseStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderProducerService {

	private final RabbitTemplate rabbitTemplate;

	// RabbitMQ 설정 상수
	private static final String EXCHANGE_NAME = "reminder.exchange";
	private static final String ROUTING_KEY = "reminder.routing.key";

	// 리마인더 메시지를 RabbitMQ에 예약
	public void scheduleReminder(Note note) {

		try {

			// GMS API 호출하여 질문 생성
			// todo: gms api 연동 후 실제 질문 생성으로 교체하기
			String question = createQuestion(note);

			ReminderMessage message = new ReminderMessage(
				note.getId(),
				note.getUser().getId(),
				note.getTitle(),
				question,
				note.getRemindAt(),
				note.getRemindCount()
			);

			long delay = getDelay(note.getRemindAt());

			// todo: 개발 완료 후 제거
			log.info("리마인더 전송하는 노트 ID: {}, delay: {}초", note.getId(), delay);

			rabbitTemplate.convertAndSend(
				EXCHANGE_NAME,
				ROUTING_KEY,
				message,
				msg -> {
					msg.getMessageProperties().setHeader("x-delay", delay);
					return msg;
				}
			);
			// todo: 삭제할 로그
			log.info("리마인더 예약 완료");
		} catch (Exception e) {
			log.error("리마인더 예약 실패한 노트 ID: {}", note.getId(), e);
			throw new BaseException(BaseResponseStatus.REMINDER_SCHEDULE_FAILED);
		}
	}

	// todo: GMS API 연동
	private String createQuestion(Note note) {
		return String.format("'%s' 노트 복습할 시간입니다.", note.getTitle());
	}

	private long getDelay(LocalDateTime remindAt) {
		long delay = Duration.between(LocalDateTime.now(), remindAt).toMillis();

		if (delay < 0) {
			//지난 시간이므로 즉시 리마인더 발송
			return 0;
		}

		return delay;
	}
}
