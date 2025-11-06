package uknowklp.secondbrain.api.note.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.note.domain.Note;
import uknowklp.secondbrain.api.note.domain.NoteDraft;
import uknowklp.secondbrain.api.note.dto.NoteRequest;
import uknowklp.secondbrain.global.exception.BaseException;

/**
 * Draft 자동 저장 스케줄러
 *
 * Batching 전략:
 * - 5분마다 오래된 Draft를 DB로 자동 저장
 * - lastModified가 5분 이상 지난 Draft 대상
 *
 * 자동 저장 트리거 (4가지):
 * 1. 프론트엔드 Batching (50회 변경 or 5분 경과)
 * 2. 페이지 이탈 (beforeunload)
 * 3. Side Peek 닫기
 * 4. 백엔드 스케줄러 (5분마다) ← 이 클래스
 *
 * 트랜잭션 전략 (v2):
 * - 각 Draft별 독립 트랜잭션 (Propagation.REQUIRES_NEW)
 * - 하나의 Draft 실패가 다른 Draft에 영향 없음
 * - 트랜잭션 경계가 명확하여 예외 처리 안정성 향상
 *
 * @see <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#tx-propagation">Spring Transaction Propagation</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteDraftAutoSaveService {

	private final NoteDraftService draftService;
	private final NoteService noteService;

	/**
	 * 5분마다 오래된 Draft를 DB로 자동 저장
	 *
	 * 실행 주기: 5분 (300,000ms)
	 * 대상: lastModified가 5분 이상 지난 Draft
	 *
	 * 이중 안전장치:
	 * - 프론트엔드에서 자동 저장 못했을 경우 백업
	 * - 네트워크 끊김이나 브라우저 강제 종료 시 보호
	 *
	 * 트랜잭션 전략 (v2):
	 * - 스케줄러 메서드 자체는 트랜잭션 없음
	 * - 각 Draft 저장은 독립 트랜잭션으로 처리
	 */
	@Scheduled(fixedDelay = 300000) // 5분
	public void autoSaveStaleDrafts() {
		try {
			log.debug("Draft 자동 저장 스케줄러 시작");

			// 5분 이상 지난 Draft 조회
			List<NoteDraft> staleDrafts = draftService.getStaleDrafts(5);

			if (staleDrafts.isEmpty()) {
				log.debug("자동 저장할 Draft 없음");
				return;
			}

			int savedCount = 0;
			int skippedCount = 0;  // 검증 실패 (정상 케이스)
			int failedCount = 0;   // 시스템 오류 (비정상 케이스)

			for (NoteDraft draft : staleDrafts) {
				try {
					// 각 Draft별 독립 트랜잭션으로 처리
					promoteDraftToDatabaseWithTransaction(draft);
					savedCount++;

				} catch (BaseException e) {
					// 비즈니스 검증 실패 - 정상적인 케이스 (빈 내용 등)
					log.warn("Draft 검증 실패로 건너뜀 - NoteId: {}, Reason: {}",
						draft.getNoteId(), e.getMessage());
					skippedCount++;

				} catch (Exception e) {
					// 시스템 오류 - 비정상 케이스 (DB 장애, Redis 오류 등)
					log.error("Draft 자동 저장 시스템 오류 - NoteId: {}",
						draft.getNoteId(), e);
					failedCount++;
				}
			}

			// 구분된 로깅으로 모니터링 품질 향상
			log.info("Draft 자동 저장 완료 - 성공: {}, 검증 건너뜀: {}, 시스템 오류: {}, 전체: {}",
				savedCount, skippedCount, failedCount, staleDrafts.size());

			// 시스템 오류가 있으면 경고 (모니터링 시스템 연동 가능)
			if (failedCount > 0) {
				log.warn("⚠️ Draft 자동 저장 중 {}건의 시스템 오류 발생 - 즉시 확인 필요", failedCount);
			}

		} catch (Exception e) {
			log.error("Draft 자동 저장 스케줄러 실패", e);
		}
	}

	/**
	 * Draft를 DB로 승격 저장 (독립 트랜잭션)
	 *
	 * Propagation.REQUIRES_NEW:
	 * - 항상 새로운 트랜잭션 생성
	 * - 다른 Draft 처리와 완전히 독립적
	 * - 이 Draft 실패가 다른 Draft에 영향 없음
	 * - 롤백 범위가 명확 (현재 Draft만 롤백)
	 *
	 * @param draft 저장할 Draft
	 * @see <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Propagation.html#REQUIRES_NEW">Propagation.REQUIRES_NEW</a>
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	protected void promoteDraftToDatabaseWithTransaction(NoteDraft draft) {
		// NoteRequest 생성 (도메인 모델 변환 메서드 사용)
		NoteRequest request = draft.toNoteRequest();

		// DB 저장 (검증 포함 - title과 content 모두 필수)
		// validateNoteRequest()에서 빈 값 체크
		Note note = noteService.createNote(draft.getUserId(), request);

		// Draft 삭제 (DB 저장 성공 후)
		// throwOnFailure=true: 삭제 실패 시 트랜잭션 롤백 (Note도 롤백)
		draftService.deleteDraft(draft.getNoteId(), draft.getUserId(), true);

		log.info("Draft 자동 저장 성공 - DraftId: {} → NoteId: {}",
			draft.getNoteId(), note.getId());
	}
}
