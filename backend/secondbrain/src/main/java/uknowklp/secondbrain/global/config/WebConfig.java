package uknowklp.secondbrain.global.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Spring MVC 설정
 *
 * HTTP Message Converter 설정:
 * - @RequestBody/@ResponseBody 직렬화/역직렬화
 * - @class 타입 정보 제거 (프론트엔드 호환성)
 * - LocalDateTime 지원 (JavaTimeModule)
 *
 * 리팩토링 (v3):
 * - configureMessageConverters → extendMessageConverters 변경
 *   - 이유: Spring Boot 기본 컨버터 유지
 *   - 효과: 안전한 확장 방식, Spring Boot Best Practice 준수
 * - ObjectMapper 주입 방식으로 변경
 *   - 이유: JacksonConfig의 공통 Bean 사용
 *   - 효과: 코드 중복 제거, 일관성 보장
 *
 * Converter 등록 순서 (v3.1):
 * - extendMessageConverters는 Spring Boot 기본 컨버터가 이미 등록된 후 호출됨
 * - 기본 순서: ByteArray(0) → String(1) → Resource(2-5) → Jackson(6)
 * - 커스텀 Jackson 컨버터를 index 6에 추가하여 기본 Jackson 컨버터를 앞서도록 함
 * - 효과: @class 제거 설정 적용, Swagger/SpringDoc 정상 작동 유지
 *
 * @see <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/config/annotation/WebMvcConfigurer.html">WebMvcConfigurer JavaDoc</a>
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	// 공통 ObjectMapper Bean 주입 (JacksonConfig에서 생성)
	private final ObjectMapper objectMapper;

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		// extendMessageConverters는 Spring Boot 기본 컨버터가 이미 등록된 후 호출됨
		// 기본 컨버터 순서 (0-6):
		// 0: ByteArrayHttpMessageConverter
		// 1: StringHttpMessageConverter
		// 2: ResourceHttpMessageConverter
		// 3: ResourceRegionHttpMessageConverter
		// 4: AllEncompassingFormHttpMessageConverter
		// 5: Jaxb2RootElementHttpMessageConverter (조건부)
		// 6: MappingJackson2HttpMessageConverter (기본 설정)

		// 커스텀 Jackson 컨버터를 index 6에 추가
		// 기본 Jackson 컨버터(6번)보다 앞에 위치하여 우선 처리
		// 효과: @class 제거 설정 적용, 프론트엔드 호환성 유지
		// 기본 ByteArray, String, Resource 컨버터는 그대로 유지 → Swagger 정상 작동
		MappingJackson2HttpMessageConverter converter =
			new MappingJackson2HttpMessageConverter(objectMapper);
		converters.add(6, converter);
	}
}
