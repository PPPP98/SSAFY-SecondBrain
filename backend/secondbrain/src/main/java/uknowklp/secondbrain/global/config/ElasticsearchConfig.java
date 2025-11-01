package uknowklp.secondbrain.global.config;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.util.StringUtils;

@Configuration
@EnableElasticsearchRepositories(basePackages = "uknowklp.secondbrain.api.note.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

	@Value("${spring.elasticsearch.uris}")
	private String uris;

	@Value("${spring.elasticsearch.username:}")
	private String username;

	@Value("${spring.elasticsearch.password:}")
	private String password;

	@Override
	public ClientConfiguration clientConfiguration() {
		// 인증 정보가 있는 경우
		if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
			return ClientConfiguration.builder()
				.connectedTo(uris)
				// usingSsl의 두 번째 인자로 HostnameVerifier를 전달하여 검증을 비활성화
				.usingSsl(createUnsafeSslContext(), (hostname, session) -> true)
				.withBasicAuth(username, password)
				.build();
		}

		// 인증 정보가 없는 경우
		return ClientConfiguration.builder()
			.connectedTo(uris)
			.build();
	}

	/**
	 * 개발 환경을 위해 모든 인증서를 신뢰하는 SSLContext를 생성합니다.
	 * <b>주의: 운영 환경에서는 절대 사용하면 안 됩니다.</b>
	 * @return SSLContext
	 */
	private SSLContext createUnsafeSslContext() {
		try {
			return SSLContextBuilder.create()
				.loadTrustMaterial(new TrustAllStrategy())
				.build();
		} catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
			throw new RuntimeException("Failed to create SSL context for Elasticsearch", e);
		}
	}
}