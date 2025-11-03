package uknowklp.secondbrain.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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
}
