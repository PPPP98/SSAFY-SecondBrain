package uknowklp.secondbrain.api.note.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Length;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uknowklp.secondbrain.api.user.domain.User;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notes")
public class Note {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "note_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Length(max = 64)
	@Column(nullable = false, length = 64)
	private String title;

	@Length(max = 2048)
	@Column(nullable = false, length = 2048)
	private String content;

	@Column(name = "remind_at")
	private LocalDateTime remindAt;

	@Column(name = "remind_count", nullable = false)
	@Builder.Default
	private Integer remindCount = 0;

	/**
	 * 노트 내용 수정
	 *
	 * @param title 수정할 제목
	 * @param content 수정할 내용
	 */
	public void update(String title, String content) {
		this.title = title;
		this.content = content;
	}

	// 리마인더 활성화
	public void enableReminder(LocalDateTime nextReminderTime) {
		this.remindCount = 0;
		this.remindAt = nextReminderTime;
	}

	// 리마인더 비활성화
	public void disableReminder() {
	this.remindAt = null;
	this.remindCount = 0;
	}

	// 다음 리마인더 예약
	public void scheduleNextReminder(LocalDateTime nextReminderTime) {
		this.remindCount ++;
		this.remindAt = nextReminderTime;
	}

	// 리마인더 3회 모두 완료 후처리
	public void completeReminder() {
		this.remindAt = null;
		this.remindCount = 0;
	}

	// 리마인더 활성화 여부 확인
	public boolean isReminderEnable() {
		return this.remindAt != null;
	}

	// 리마인더 발송 가능 여부 확인
	public boolean isReminderReady() {
		return this.user.isSetAlarm()
			&& this.remindAt != null
			&& this.remindAt.isBefore(LocalDateTime.now());
	}
}
