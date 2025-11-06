package uknowklp.secondbrain.api.note.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Draft 저장 요청 DTO
 *
 * 검증 전략 (v2):
 * - Redis 저장: title 또는 content 중 하나라도 있으면 허용
 * - DB 저장: 둘 다 필수 (NoteService.validateNoteRequest()에서 검증)
 * - Version: 필수 (동시 편집 충돌 감지를 위한 Optimistic Locking)
 *
 * 버전 충돌 감지 강화:
 * - version 필드를 필수로 변경하여 데이터 손실 방지
 * - 새 Draft는 version = 1로 시작
 * - 기존 Draft 수정 시 현재 version 전송 필수
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoteDraftRequest {

	/**
	 * 노트 ID (기존 노트) 또는 null (새 노트)
	 * null인 경우 서버에서 UUID 생성
	 */
	private String noteId;

	/**
	 * 제목 (타이핑 중 빈 값 허용, Redis에만 저장)
	 * DB 저장 시 NoteService에서 @NotBlank 검증
	 */
	@Size(max = 64, message = "제목은 64자를 초과할 수 없습니다")
	private String title;

	/**
	 * 내용 (타이핑 중 빈 값 허용, Redis에만 저장)
	 * DB 저장 시 NoteService에서 @NotBlank 검증
	 * TEXT 타입으로 길이 제한 없음
	 */
	private String content;

	/**
	 * 버전 (충돌 감지용, 필수)
	 *
	 * Optimistic Locking 전략:
	 * - 새 Draft: version = 1
	 * - 기존 Draft 수정: 현재 version 전송 필수
	 * - 서버에서 버전 불일치 시 DRAFT_VERSION_CONFLICT 에러 반환
	 *
	 * 동시 편집 보호:
	 * - 사용자 A가 브라우저 1에서 편집 → version 증가
	 * - 사용자 A가 브라우저 2에서 이전 version으로 저장 시도 → 충돌 감지
	 * - 데이터 손실 방지
	 */
	@NotNull(message = "버전 정보는 필수입니다")
	private Long version;

	/**
	 * Draft 유효성 검사 (최소 요구사항)
	 *
	 * title 또는 content 중 하나라도 있어야 함
	 * 둘 다 비어있으면 Redis에 저장할 필요 없음
	 *
	 * @return 유효 여부
	 */
	public boolean isValid() {
		return (title != null && !title.trim().isEmpty()) ||
			(content != null && !content.trim().isEmpty());
	}
}
