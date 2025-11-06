package uknowklp.secondbrain.api.note.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Delta 변경 작업 (미래 확장용)
 *
 * 현재는 사용하지 않지만, 향후 실시간 협업이나
 * 변경 이력 추적이 필요할 때 활성화
 *
 * Delta-Based Saving 전략:
 * - 전체 문서가 아닌 변경된 부분만 전송하여 네트워크 효율성 향상
 * - Operational Transform (OT) 또는 CRDT 알고리즘과 결합 가능
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeltaOperation {

	/**
	 * Delta 변경 타입
	 */
	public enum DeltaType {
		/** 텍스트 삽입 */
		INSERT,
		/** 텍스트 삭제 */
		DELETE,
		/** 텍스트 교체 */
		REPLACE
	}

	/** Delta 타입 */
	private DeltaType type;

	/** 변경 위치 (문자 인덱스) */
	private Integer position;

	/** 이전 값 (DELETE, REPLACE 시 사용) */
	private String oldValue;

	/** 새 값 (INSERT, REPLACE 시 사용) */
	private String newValue;

	/** 타임스탬프 (순서 보장용) */
	private Long timestamp;
}
