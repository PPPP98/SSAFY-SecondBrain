package uknowklp.secondbrain.api.note.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.gms.service.GmsQuestionService;
import uknowklp.secondbrain.api.note.domain.Note;
import uknowklp.secondbrain.api.note.repository.NoteRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderSchedulerService {

	private final NoteRepository noteRepository;
	private final GmsQuestionService gmsQuestionService;

	private static final int MAX_REMINDER_COUNT = 3;

	// 10초마다 실행 (이전 실행 완료 후 10초 대기)
	@Scheduled(fixedDelay = 10000)
	@Transactional
	public void checkAndSendReminders() {
		LocalDateTime now = LocalDateTime.now();

		// 발송 대상 조회
		List<Note> pendingNotes = noteRepository.findPendingReminders(now, MAX_REMINDER_COUNT);

		if (pendingNotes.isEmpty()) {
			return;
		}

		log.info("리마인더 발송 대상 {}개 발견", pendingNotes.size());

		// 각 노트 처리
		for (Note note : pendingNotes) {
			try {
				processReminder(note);
			} catch (Exception e) {
				log.error("리마인더 발송 실패 - noteId: {}", note.getId(), e);
			}
		}
	}

	// 개별 노트 리마인더 처리
	private void processReminder(Note note) {
		// User 알림 설정 확인
		if (!note.getUser().isSetAlarm()) {
			log.warn("사용자 알림 OFF - noteId: {}, userId: {}", note.getId(), note.getUser().getId());
			return;
		}

		int currentCount = note.getRemindCount();

		// GMS 비동기 호출로 질문 생성
		gmsQuestionService.makeReminderQuestion(note)
			.doOnSuccess(question -> {
				log.info("GMS 질문 생성 완료 - noteId: {}, question: \"{}\"", note.getId(), question);

				// TODO: 실제 알림 발송 (FCM, WebSocket 등 구현 후 작성)
				// notificationService.sendReminder(note.getUser(), question);

				log.info("리마인더 발송 완료 - noteId: {}, 횟수: {}/3", note.getId(), currentCount + 1);
			})
			.doOnError(e -> log.error("GMS 질문 생성 실패 - noteId: {}", note.getId(), e))
			.subscribe(); // 비동기 실행

		// 다음 리마인더 시간 계산 및 저장
		try {
			if (currentCount >= 2) {
				// 3회 완료
				note.completeReminder();
				log.info("리마인더 완료 (3회) - noteId: {}", note.getId());
			} else {
				// 다음 발송 시간 설정
				// todo: 실제 서비스에서는 1일, 3일 ,7일로 설정 예정
				int nextDelaySeconds = switch (currentCount) {
					case 0 -> 30;  // 1차 → 2차: 30초 후
					case 1 -> 70;  // 2차 → 3차: 70초 후
					default -> 0;
				};

				LocalDateTime nextTime = LocalDateTime.now().plusSeconds(nextDelaySeconds);
				note.scheduleNextReminder(nextTime);
				log.info("다음 리마인더 예약 - noteId: {}, 시간: {}", note.getId(), nextTime);
			}

			noteRepository.save(note);

		} catch (OptimisticLockException e) {
			// 다른 스케줄러 인스턴스가 먼저 처리함 (정상 상황)
			log.info("동시성 충돌 감지 - noteId: {} (다른 인스턴스가 이미 처리함)", note.getId());
		}
	}
}
