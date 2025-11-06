package uknowklp.secondbrain.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Scheduler 설정
 *
 * NoteDraftAutoSaveService의 @Scheduled 활성화
 *
 * 스케줄러 동작:
 * - @Scheduled(fixedDelay = 300000) → 5분마다 실행
 * - 이전 실행 완료 후 5분 대기 후 다음 실행
 * - 비동기 실행으로 메인 애플리케이션에 영향 없음
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
	// @EnableScheduling 활성화로 @Scheduled 메서드 자동 실행
}
