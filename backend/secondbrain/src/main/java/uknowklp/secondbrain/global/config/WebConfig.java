package uknowklp.secondbrain.global.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Spring MVC 설정
 *
 * HTTP Message Converter 설정:
 * - @RequestBody/@ResponseBody 직렬화/역직렬화
 * - @class 타입 정보 제거 (프론트엔드 호환성)
 * - LocalDateTime 지원 (JavaTimeModule)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		// 기본 ObjectMapper 생성 (@ class 없음)
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		// activateDefaultTyping 호출하지 않음 → @class 불필요

		// Jackson HTTP Message Converter 추가
		MappingJackson2HttpMessageConverter converter =
			new MappingJackson2HttpMessageConverter(objectMapper);
		converters.add(0, converter); // 맨 앞에 추가 (우선순위 높음)
	}
}
