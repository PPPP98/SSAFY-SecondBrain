package uknowklp.secondbrain.api.note.domain;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uknowklp.secondbrain.api.note.dto.DeltaOperation;

/**
 * 노트 임시 저장 (Redis)
 *
 * Redis Key Pattern: draft:note:{noteId}
 * TTL: 24시간 (86400초)
 *
 * Hybrid Snapshot + Delta 방식:
 * - Primary: Snapshot (전체 title + content 저장)
 * - Secondary: Delta (미래 확장용, 현재 null)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoteDraft {

	/** 노트 ID (DB의 note_id와 동일, 또는 UUID for new notes) */
	private String noteId;

	/** 사용자 ID */
	private Long userId;

	/** 제목 (최대 64자) */
	private String title;

	/** 내용 (TEXT 타입, 제한 없음) */
	private String content;

	/** 버전 (Optimistic Locking, 충돌 감지용) */
	@Builder.Default
	private Long version = 1L;

	/** 마지막 수정 시간 */
	@Builder.Default
	private LocalDateTime lastModified = LocalDateTime.now();

	/**
	 * Delta 목록 (미래 확장용, 현재 null)
	 *
	 * 향후 실시간 협업이나 변경 이력 추적이 필요할 때 활성화
	 */
	private List<DeltaOperation> deltas;

	/**
	 * 버전 증가
	 * 매 변경마다 호출되어 충돌 감지에 사용
	 */
	public void incrementVersion() {
		this.version++;
		this.lastModified = LocalDateTime.now();
	}

	/**
	 * 내용 업데이트
	 *
	 * @param title   수정할 제목
	 * @param content 수정할 내용
	 */
	public void updateContent(String title, String content) {
		this.title = title;
		this.content = content;
		incrementVersion();
	}

	/**
	 * NoteRequest로 변환
	 *
	 * Draft를 DB에 저장하기 위한 NoteRequest 생성
	 * DRY 원칙: 변환 로직을 도메인 모델에 캡슐화
	 *
	 * @return NoteRequest
	 */
	public uknowklp.secondbrain.api.note.dto.NoteRequest toNoteRequest() {
		return uknowklp.secondbrain.api.note.dto.NoteRequest.builder()
			.title(this.title)
			.content(this.content)
			.build();
	}
}
