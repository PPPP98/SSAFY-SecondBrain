package uknowklp.secondbrain.api.note.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.note.constant.DraftProcessingStatus;
import uknowklp.secondbrain.api.note.domain.NoteDraft;
import uknowklp.secondbrain.api.note.dto.NoteDraftRequest;
import uknowklp.secondbrain.api.note.dto.NoteDraftResponse;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponseStatus;

/**
 * NoteDraft Redis 관리 서비스
 *
 * Write-Behind 패턴:
 * 1. 프론트엔드 Debouncing (500ms)
 * 2. Redis 임시 저장 (TTL 24h)
 * 3. 자동 저장 트리거 (50회 변경 or 5분 경과 or 페이지 이탈 or Side Peek 닫기)
 * 4. DB 영구 저장
 *
 * 성능 최적화 (v2):
 * - 타입 특화 RedisTemplate<String, NoteDraft> 사용으로 직렬화 성능 개선
 * - KEYS 명령어 제거 → 사용자별 SET 관리로 O(1) 성능 달성
 * - ObjectMapper 변환 과정 제거로 CPU 사용량 감소
 *
 * @see <a href="https://redis.io/docs/latest/commands/scan">Redis SCAN vs KEYS Performance</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteDraftService {

	private final RedisTemplate<String, NoteDraft> noteDraftRedisTemplate;
	private final RedisTemplate<String, Object> redisTemplate; // SET 관리용
	private final StringRedisTemplate stringRedisTemplate; // 처리 상태 추적용

	// Redis Key Patterns
	private static final String DRAFT_PREFIX = "draft:note:";
	private static final String USER_DRAFTS_PREFIX = "user:drafts:";
	private static final String PROCESSED_PREFIX = "processed:draft:";

	// TTL: 24시간
	private static final Duration DRAFT_TTL = Duration.ofHours(24);
	// 처리 완료 기록 TTL: 24시간 (Draft와 동일하게 설정하여 완벽한 중복 방지)
	private static final Duration PROCESSED_TTL = Duration.ofHours(24);

	/**
	 * Draft 저장 (Redis)
	 *
	 * 검증 전략: title 또는 content 중 하나라도 있으면 저장
	 *
	 * 성능 최적화 (v2):
	 * - 사용자별 SET에 noteId 추가하여 O(1) 조회 성능 보장
	 * - KEYS 명령어 사용 불필요
	 *
	 * 성능 최적화 (v3):
	 * - NoteDraft 객체 직접 반환으로 Redis 재조회 제거
	 * - Controller에서 getDraft() 불필요 → Redis 네트워크 호출 50% 감소
	 * - 자동 저장 빈번한 시나리오에서 성능 향상 (100회 저장 시 100회 Redis 호출 제거)
	 *
	 * @param userId  사용자 ID
	 * @param request Draft 요청
	 * @return 저장된 NoteDraft 객체 (version 포함)
	 */
	public NoteDraft saveDraft(Long userId, NoteDraftRequest request) {
		try {
			// 최소 검증: title 또는 content 중 하나라도 있어야 함
			if (!request.isValid()) {
				log.warn("Draft 저장 실패 - 빈 내용 - UserId: {}", userId);
				throw new BaseException(BaseResponseStatus.DRAFT_EMPTY);
			}

			// noteId가 없으면 새 UUID 생성 (새 노트)
			String noteId = request.getNoteId() != null
				? request.getNoteId()
				: UUID.randomUUID().toString();

			// 기존 draft 조회 (충돌 감지)
			NoteDraft existingDraft = getDraftOrNull(noteId);

			// Version 충돌 검사 (Optimistic Locking)
			// v2: version 필수화로 항상 검증 수행
			if (existingDraft != null) {
				// 기존 Draft가 존재하는 경우: version 일치 여부 확인
				if (!existingDraft.getVersion().equals(request.getVersion())) {
					log.warn("Version conflict - NoteId: {}, Client: {}, Server: {}",
						noteId, request.getVersion(), existingDraft.getVersion());
					throw new BaseException(BaseResponseStatus.DRAFT_VERSION_CONFLICT);
				}
			} else {
				// 새 Draft인 경우: version은 1이어야 함
				if (request.getVersion() != 1L) {
					log.warn("Invalid initial version - NoteId: {}, Version: {}",
						noteId, request.getVersion());
					throw new BaseException(BaseResponseStatus.DRAFT_INVALID_VERSION);
				}
			}

			// NoteDraft 생성 또는 업데이트
			NoteDraft draft = existingDraft != null
				? updateExistingDraft(existingDraft, request)
				: createNewDraft(noteId, userId, request);

			// Redis 저장 (Draft 데이터)
			String draftKey = DRAFT_PREFIX + noteId;
			noteDraftRedisTemplate.opsForValue().set(draftKey, draft, DRAFT_TTL);

			// 사용자별 Draft SET에 추가 (성능 최적화)
			String userDraftsKey = USER_DRAFTS_PREFIX + userId;
			redisTemplate.opsForSet().add(userDraftsKey, noteId);
			redisTemplate.expire(userDraftsKey, DRAFT_TTL);

			log.info("Draft 저장 완료 - NoteId: {}, UserId: {}, Version: {}",
				noteId, userId, draft.getVersion());

			// NoteDraft 객체 직접 반환 (Redis 재조회 불필요)
			return draft;

		} catch (BaseException e) {
			throw e;
		} catch (Exception e) {
			log.error("Draft 저장 실패 - UserId: {}", userId, e);
			throw new BaseException(BaseResponseStatus.REDIS_ERROR);
		}
	}

	/**
	 * Draft 조회 (Redis)
	 *
	 * @param noteId 노트 ID
	 * @param userId 사용자 ID
	 * @return NoteDraft
	 */
	public NoteDraft getDraft(String noteId, Long userId) {
		NoteDraft draft = getDraftOrNull(noteId);

		if (draft == null) {
			log.warn("Draft 없음 - NoteId: {}, UserId: {}", noteId, userId);
			throw new BaseException(BaseResponseStatus.DRAFT_NOT_FOUND);
		}

		// 소유권 검증
		if (!draft.getUserId().equals(userId)) {
			log.warn("Draft 접근 권한 없음 - NoteId: {}, UserId: {}", noteId, userId);
			throw new BaseException(BaseResponseStatus.DRAFT_ACCESS_DENIED);
		}

		return draft;
	}

	/**
	 * 사용자의 모든 Draft 목록 조회 (Redis)
	 *
	 * 사용 시나리오:
	 * - 브라우저 재시작 후 미저장 Draft 복구
	 * - 여러 노트를 동시에 작성 중인 경우
	 *
	 * 성능 최적화 (v3):
	 * - KEYS 명령어 제거 → O(N) → O(1) 성능 개선
	 * - 사용자별 SET에서 직접 조회로 Redis 부하 최소화
	 * - N+1 문제 해결 → MGET 사용으로 배치 조회
	 * - 이전: 1 + M번 호출 → 개선: 2번 호출 (SMEMBERS + MGET)
	 *
	 * @param userId 사용자 ID
	 * @return Draft 목록 (lastModified 기준 내림차순)
	 * @see <a href="https://redis.io/docs/latest/commands/smembers">Redis SMEMBERS</a>
	 * @see <a href="https://redis.io/docs/latest/commands/mget">Redis MGET Command</a>
	 */
	public List<NoteDraftResponse> listUserDrafts(Long userId) {
		try {
			// 1단계: 사용자별 SET에서 noteId 목록 조회
			String userDraftsKey = USER_DRAFTS_PREFIX + userId;
			Set<Object> noteIdSet = redisTemplate.opsForSet().members(userDraftsKey);

			if (noteIdSet == null || noteIdSet.isEmpty()) {
				log.debug("Draft 목록 없음 - UserId: {}", userId);
				return Collections.emptyList();
			}

			// 2단계: noteId → Redis key 변환
			List<String> draftKeys = noteIdSet.stream()
				.map(Object::toString)
				.map(noteId -> DRAFT_PREFIX + noteId)
				.collect(Collectors.toList());

			// 3단계: MGET으로 한 번에 조회 (N+1 문제 해결)
			List<NoteDraft> drafts = noteDraftRedisTemplate.opsForValue().multiGet(draftKeys);

			if (drafts == null) {
				return Collections.emptyList();
			}

			// 4단계: 정렬 및 변환
			List<NoteDraftResponse> responses = drafts.stream()
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(NoteDraft::getLastModified).reversed())
				.map(NoteDraftResponse::from)
				.collect(Collectors.toList());

			log.info("Draft 목록 조회 완료 - UserId: {}, Count: {}", userId, responses.size());
			return responses;

		} catch (Exception e) {
			log.error("Draft 목록 조회 실패 - UserId: {}", userId, e);
			return Collections.emptyList();
		}
	}

	/**
	 * Draft 삭제 (Redis)
	 *
	 * 성능 최적화 (v2):
	 * - 사용자별 SET에서도 noteId 제거하여 일관성 유지
	 *
	 * @param noteId 노트 ID
	 * @param userId 사용자 ID
	 */
	public void deleteDraft(String noteId, Long userId) {
		deleteDraft(noteId, userId, false);
	}

	/**
	 * Draft 삭제 (Redis) - 트랜잭션 안전 버전
	 *
	 * 성능 최적화 (v3):
	 * - 사용자별 SET에서도 noteId 제거하여 일관성 유지
	 * - 트랜잭션 내에서 호출 시 예외 전파 옵션 제공
	 *
	 * @param noteId         노트 ID
	 * @param userId         사용자 ID
	 * @param throwOnFailure true면 실패 시 예외 발생 (트랜잭션 롤백용)
	 */
	public void deleteDraft(String noteId, Long userId, boolean throwOnFailure) {
		try {
			// 소유권 검증
			NoteDraft draft = getDraft(noteId, userId);

			// Draft 데이터 삭제
			String draftKey = DRAFT_PREFIX + noteId;
			Boolean deleted = noteDraftRedisTemplate.delete(draftKey);

			// 사용자별 SET에서도 제거
			String userDraftsKey = USER_DRAFTS_PREFIX + userId;
			redisTemplate.opsForSet().remove(userDraftsKey, noteId);

			log.info("Draft 삭제 완료 - NoteId: {}, UserId: {}, Deleted: {}",
				noteId, userId, deleted);

		} catch (Exception e) {
			log.error("Draft 삭제 실패 - NoteId: {}, UserId: {}", noteId, userId, e);

			if (throwOnFailure) {
				// 트랜잭션 내에서 호출 시 예외 전파 (롤백 유도)
				throw new BaseException(BaseResponseStatus.REDIS_ERROR);
			}
			// 일반 호출 시 Best-effort 삭제 (실패해도 TTL로 자동 삭제됨)
		}
	}

	/**
	 * Draft 존재 여부 확인
	 *
	 * @param noteId 노트 ID
	 * @return 존재 여부
	 */
	public boolean existsDraft(String noteId) {
		String key = DRAFT_PREFIX + noteId;
		return Boolean.TRUE.equals(noteDraftRedisTemplate.hasKey(key));
	}

	/**
	 * 오래된 Draft 조회 (자동 저장용)
	 *
	 * Batching 전략: 5분마다 백엔드 스케줄러가 호출
	 *
	 * 성능 최적화 (v3):
	 * - KEYS 명령어 제거 → SCAN 명령어로 전환
	 * - N+1 문제 해결 → MGET 사용으로 배치 조회
	 * - 이전: 1 + N번 호출 → 개선: 2번 호출 (SCAN + MGET)
	 * - Redis 블로킹 없이 cursor 기반 안전한 스캔
	 *
	 * @param minutes lastModified가 이 시간보다 오래된 Draft
	 * @return 오래된 Draft 목록
	 * @see <a href="https://redis.io/docs/latest/commands/scan">Redis SCAN Command</a>
	 * @see <a href="https://redis.io/docs/latest/commands/mget">Redis MGET Command</a>
	 */
	public List<NoteDraft> getStaleDrafts(int minutes) {
		try {
			// 1단계: SCAN으로 모든 key 수집
			List<String> allKeys = new ArrayList<>();
			ScanOptions options = ScanOptions.scanOptions()
				.match(DRAFT_PREFIX + "*")
				.count(100) // 한 번에 스캔할 키 개수
				.build();

			try (Cursor<String> cursor = noteDraftRedisTemplate.scan(options)) {
				while (cursor.hasNext()) {
					try {
						allKeys.add(cursor.next());
					} catch (Exception e) {
						log.warn("Draft 스캔 중 개별 오류 발생 - 건너뜀", e);
					}
				}
			}

			if (allKeys.isEmpty()) {
				log.debug("오래된 Draft 조회 - Draft 없음");
				return Collections.emptyList();
			}

			// 2단계: MGET으로 한 번에 조회 (N+1 문제 해결)
			List<NoteDraft> allDrafts = noteDraftRedisTemplate.opsForValue().multiGet(allKeys);

			if (allDrafts == null) {
				return Collections.emptyList();
			}

			// 3단계: 시간 기준 필터링
			LocalDateTime threshold = LocalDateTime.now().minusMinutes(minutes);
			List<NoteDraft> staleDrafts = allDrafts.stream()
				.filter(Objects::nonNull)
				.filter(draft -> draft.getLastModified().isBefore(threshold))
				.collect(Collectors.toList());

			log.debug("오래된 Draft 조회 - Threshold: {}분, 전체: {}, 필터링 후: {}",
				minutes, allKeys.size(), staleDrafts.size());
			return staleDrafts;

		} catch (Exception e) {
			log.error("오래된 Draft 조회 실패", e);
			return Collections.emptyList();
		}
	}

	// ===== Private Helper Methods =====

	/**
	 * Draft 조회 (소유권 검증 없음)
	 *
	 * 성능 최적화 (v2):
	 * - 타입 특화 RedisTemplate 사용으로 직접 NoteDraft 반환
	 * - ObjectMapper.convertValue() 변환 과정 제거
	 * - CPU 사용량 감소 및 응답 시간 개선
	 *
	 * @param key Redis key 또는 noteId
	 * @return NoteDraft 또는 null
	 */
	private NoteDraft getDraftOrNull(String key) {
		try {
			if (!key.startsWith(DRAFT_PREFIX)) {
				key = DRAFT_PREFIX + key;
			}

			// 타입 특화 RedisTemplate이 직접 NoteDraft 반환 (변환 불필요)
			return noteDraftRedisTemplate.opsForValue().get(key);

		} catch (Exception e) {
			log.error("Draft 조회 실패 - Key: {}", key, e);
			return null;
		}
	}

	/**
	 * 새 Draft 생성
	 */
	private NoteDraft createNewDraft(String noteId, Long userId, NoteDraftRequest request) {
		return NoteDraft.builder()
			.noteId(noteId)
			.userId(userId)
			.title(request.getTitle())
			.content(request.getContent())
			.version(1L)
			.lastModified(LocalDateTime.now())
			.deltas(null) // 미래 확장용
			.build();
	}

	/**
	 * 기존 Draft 업데이트
	 */
	private NoteDraft updateExistingDraft(NoteDraft existing, NoteDraftRequest request) {
		existing.updateContent(request.getTitle(), request.getContent());
		return existing;
	}

	// ===== Draft 처리 상태 관리 (멱등성 보장) =====

	/**
	 * Draft 처리 상태 확인
	 *
	 * @param draftId Draft UUID (String)
	 * @return 처리 상태 (null=미처리, "PROCESSING"=처리중, "{dbNoteId}"=완료)
	 */
	public String getProcessingStatus(String draftId) {
		String key = PROCESSED_PREFIX + draftId;
		return stringRedisTemplate.opsForValue().get(key);
	}

	/**
	 * Draft 처리 시작 표시 (원자적)
	 *
	 * Spring Data Redis Transaction 패턴 사용:
	 * - WATCH로 낙관적 락 구현
	 * - MULTI/EXEC로 원자성 보장
	 *
	 * @param draftId Draft UUID (String)
	 * @return 성공 여부 (false면 이미 처리 중)
	 * @throws BaseException Redis 오류 시
	 */
	public boolean markAsProcessing(String draftId) {
		String processedKey = PROCESSED_PREFIX + draftId;

		try {
			Boolean success = stringRedisTemplate.execute(
				new SessionCallback<Boolean>() {
					@Override
					public Boolean execute(RedisOperations operations)
						throws DataAccessException {

						// WATCH: 낙관적 락 설정
						operations.watch(processedKey);

						// 이미 처리 중이거나 완료된 Draft인지 확인
						String currentStatus = (String) operations.opsForValue()
							.get(processedKey);

						if (currentStatus != null) {
							operations.unwatch();
							log.warn("Draft 이미 처리 중 - DraftId: {}, Status: {}",
								draftId, currentStatus);
							return false;
						}

						// MULTI: 트랜잭션 시작
						operations.multi();

						// SET processed:draft:{uuid} "PROCESSING"
						operations.opsForValue().set(
							processedKey,
							DraftProcessingStatus.PROCESSING,
							PROCESSED_TTL
						);

						// EXEC: 원자적 실행
						List<Object> results = operations.exec();

						// Transaction 실패 (WATCH된 키가 변경됨)
						if (results == null || results.isEmpty()) {
							log.warn("Draft 처리 상태 기록 실패 (경쟁 조건) - DraftId: {}",
								draftId);
							return false;
						}

						log.info("Draft 처리 시작 표시 - DraftId: {}", draftId);
						return true;
					}
				}
			);

			return Boolean.TRUE.equals(success);

		} catch (Exception e) {
			log.error("Draft 처리 상태 기록 실패 - DraftId: {}", draftId, e);
			throw new BaseException(BaseResponseStatus.REDIS_ERROR);
		}
	}

	/**
	 * Draft 처리 완료 기록
	 *
	 * @param draftId  Draft UUID (String)
	 * @param dbNoteId DB에 저장된 Note ID (Long)
	 */
	public void markAsCompleted(String draftId, Long dbNoteId) {
		String key = PROCESSED_PREFIX + draftId;

		try {
			stringRedisTemplate.opsForValue().set(
				key,
				String.valueOf(dbNoteId),
				PROCESSED_TTL
			);

			log.info("Draft 처리 완료 기록 - DraftId: {}, DB NoteId: {}",
				draftId, dbNoteId);

		} catch (Exception e) {
			// 처리 완료 기록 실패해도 치명적이지 않음
			// Draft는 이미 DB에 저장됨
			log.error("Draft 처리 완료 기록 실패 - DraftId: {}", draftId, e);
		}
	}

	/**
	 * Draft 처리 상태 초기화 (롤백용)
	 *
	 * @param draftId Draft UUID (String)
	 */
	public void rollbackProcessingStatus(String draftId) {
		String key = PROCESSED_PREFIX + draftId;

		try {
			stringRedisTemplate.delete(key);
			log.info("Draft 처리 상태 롤백 - DraftId: {}", draftId);

		} catch (Exception e) {
			log.error("Draft 처리 상태 롤백 실패 - DraftId: {}", draftId, e);
		}
	}
}
