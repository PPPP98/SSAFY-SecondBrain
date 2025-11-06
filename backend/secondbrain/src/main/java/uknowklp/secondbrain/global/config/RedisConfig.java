package uknowklp.secondbrain.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import uknowklp.secondbrain.api.note.domain.NoteDraft;

/**
 * Redis 설정
 *
 * RedisTemplate<String, Object> 빈을 생성하여 다양한 타입의 데이터를 Redis에 저장할 수 있도록 합니다.
 *
 * Serialization 전략:
 * - Key: StringRedisSerializer (사람이 읽기 쉬운 문자열)
 * - Value: GenericJackson2JsonRedisSerializer (JSON 직렬화, 타입 정보 포함)
 *
 * Spring Data Redis Best Practices:
 * - RedisConnectionFactory는 Spring Boot가 application.yml 설정을 기반으로 자동 생성
 * - GenericJackson2JsonRedisSerializer를 사용하여 다양한 타입(String, Map, etc.) 처리
 * - 타입 특화 RedisTemplate은 성능 최적화와 타입 안정성 제공
 * - afterPropertiesSet()으로 초기화 보장
 *
 * @see <a href="https://docs.spring.io/spring-data/redis/docs/current/reference/html/#redis:template">Spring Data Redis - RedisTemplate</a>
 */
@Configuration
public class RedisConfig {

	/**
	 * RedisTemplate<String, Object> 빈 생성
	 *
	 * 이 템플릿은 다음 용도로 사용됩니다:
	 * - JWT Refresh Token 저장 (Map<String, Object>)
	 * - Token Blacklist 관리 (String)
	 * - OAuth2 인증 코드 저장 (String)
	 *
	 * @param connectionFactory Spring Boot가 자동 생성한 RedisConnectionFactory
	 * @return 설정된 RedisTemplate 인스턴스
	 */
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// Key Serializer: String으로 직렬화
		// Redis에서 키를 사람이 읽을 수 있는 문자열로 저장
		StringRedisSerializer stringSerializer = new StringRedisSerializer();
		template.setKeySerializer(stringSerializer);
		template.setHashKeySerializer(stringSerializer);

		// Value Serializer: JSON으로 직렬화
		// GenericJackson2JsonRedisSerializer는 타입 정보(@class)를 포함하여
		// 다양한 타입의 객체를 안전하게 직렬화/역직렬화할 수 있습니다.
		GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
		template.setValueSerializer(jsonSerializer);
		template.setHashValueSerializer(jsonSerializer);

		// 템플릿 초기화
		template.afterPropertiesSet();

		return template;
	}

	/**
	 * Redis 직렬화용 ObjectMapper 빈 생성
	 *
	 * LocalDateTime 직렬화 지원:
	 * - JavaTimeModule 등록으로 LocalDateTime, ZonedDateTime 등 지원
	 * - ISO-8601 포맷 사용 (타임스탬프 배열 방지)
	 * - 이전: [2025, 11, 6, 14, 30] → 개선: "2025-11-06T14:30:00"
	 *
	 * 재사용성:
	 * - NoteDraft RedisTemplate에서 사용
	 * - 향후 다른 도메인의 RedisTemplate에서도 재사용 가능
	 *
	 * @return LocalDateTime 직렬화를 지원하는 ObjectMapper
	 * @see <a href="https://github.com/FasterXML/jackson-modules-java8">Jackson Java 8 Modules</a>
	 */
	@Bean
	public ObjectMapper redisObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();

		// JavaTimeModule 등록 (LocalDateTime, ZonedDateTime 등 지원)
		objectMapper.registerModule(new JavaTimeModule());

		// 타임스탬프 대신 ISO-8601 포맷 사용
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		// 필드 접근 가능하도록 설정
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

		// 타입 정보 포함 (다형성 지원)
		objectMapper.activateDefaultTyping(
			LaissezFaireSubTypeValidator.instance,
			ObjectMapper.DefaultTyping.NON_FINAL,
			JsonTypeInfo.As.PROPERTY
		);

		return objectMapper;
	}

	/**
	 * NoteDraft 전용 타입 특화 RedisTemplate 빈 생성
	 *
	 * 성능 최적화 및 타입 안정성을 위한 전용 템플릿:
	 * - NoteDraft 객체를 직접 반환하여 불필요한 타입 변환 제거
	 * - Jackson2JsonRedisSerializer로 NoteDraft 타입에 최적화된 직렬화
	 * - GenericJackson2JsonRedisSerializer보다 성능 우수
	 * - ObjectMapper.convertValue() 변환 과정 불필요
	 *
	 * LocalDateTime 직렬화 (v3):
	 * - redisObjectMapper Bean 사용으로 설정 재사용
	 * - ISO-8601 포맷으로 안전한 직렬화
	 *
	 * 사용처:
	 * - NoteDraftService의 모든 Draft 저장/조회 작업
	 * - NoteDraftAutoSaveService의 자동 저장 스케줄러
	 *
	 * @param connectionFactory Spring Boot가 자동 생성한 RedisConnectionFactory
	 * @param redisObjectMapper Redis 직렬화용 ObjectMapper
	 * @return NoteDraft 타입 특화 RedisTemplate 인스턴스
	 * @see <a href="https://docs.spring.io/spring-data/redis/docs/current/reference/html/#redis:serializer">Redis Serializers</a>
	 */
	@Bean
	public RedisTemplate<String, NoteDraft> noteDraftRedisTemplate(
		RedisConnectionFactory connectionFactory,
		ObjectMapper redisObjectMapper) {

		RedisTemplate<String, NoteDraft> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// Key Serializer: String으로 직렬화
		StringRedisSerializer stringSerializer = new StringRedisSerializer();
		template.setKeySerializer(stringSerializer);
		template.setHashKeySerializer(stringSerializer);

		// Value Serializer: NoteDraft 타입 특화 Jackson 직렬화
		// redisObjectMapper 재사용으로 설정 중복 제거
		Jackson2JsonRedisSerializer<NoteDraft> jsonSerializer =
			new Jackson2JsonRedisSerializer<>(redisObjectMapper, NoteDraft.class);

		template.setValueSerializer(jsonSerializer);
		template.setHashValueSerializer(jsonSerializer);

		// 템플릿 초기화
		template.afterPropertiesSet();

		return template;
	}
}
