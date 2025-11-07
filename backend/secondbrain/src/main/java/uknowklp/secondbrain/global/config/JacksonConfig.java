package uknowklp.secondbrain.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson ObjectMapper 공통 설정
 *
 * HTTP 및 Redis에서 공통으로 사용할 ObjectMapper Bean 제공:
 * - LocalDateTime ISO-8601 포맷 지원
 * - @class 타입 정보 제거 (프론트엔드 호환성)
 * - 코드 중복 제거 (DRY 원칙)
 *
 * 사용처:
 * - WebConfig: HTTP Message Converter
 * - RedisConfig: NoteDraft 직렬화
 *
 * 리팩토링 배경:
 * - 기존: WebConfig와 RedisConfig에서 동일한 ObjectMapper 설정 중복
 * - 개선: 공통 Bean으로 추출하여 유지보수성 향상
 * - 효과: 설정 변경 시 한 곳만 수정, 일관성 보장
 *
 * @see <a href="https://github.com/FasterXML/jackson-modules-java8">Jackson Java 8 Modules</a>
 */
@Configuration
public class JacksonConfig {

	/**
	 * HTTP 및 Redis에서 공통으로 사용할 기본 ObjectMapper
	 *
	 * 설정:
	 * - JavaTimeModule: LocalDateTime, ZonedDateTime 등 Java 8 날짜/시간 타입 지원
	 * - WRITE_DATES_AS_TIMESTAMPS 비활성화: ISO-8601 포맷 사용
	 * - @class 타입 정보 없음: 프론트엔드 호환성 (activateDefaultTyping 호출하지 않음)
	 *
	 * 직렬화 예시:
	 * - 이전 (타임스탬프): [2025, 11, 6, 14, 30, 0]
	 * - 개선 (ISO-8601): "2025-11-06T14:30:00"
	 *
	 * @return 설정된 ObjectMapper 인스턴스
	 */
	@Bean
	@Primary
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();

		// JavaTimeModule 등록 (LocalDateTime, ZonedDateTime 등 지원)
		mapper.registerModule(new JavaTimeModule());

		// 타임스탬프 대신 ISO-8601 포맷 사용
		// 프론트엔드에서 읽기 쉽고 표준 포맷
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		// activateDefaultTyping 호출하지 않음
		// → @class 타입 정보 제거 (프론트엔드 호환성)

		return mapper;
	}
}
