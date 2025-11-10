package uknowklp.secondbrain.global.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Configuration
public class GmsConfig {

	@Value("${gms.api-key}")
	private String apiKey;

	@Value("${gms.base-url}")
	private String baseUrl;

	@Bean
	public WebClient gmsWebClient() {
		// Netty HttpClient 타임아웃 설정
		HttpClient httpClient = HttpClient.create()
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
			.responseTimeout(Duration.ofSeconds(30))
			.doOnConnected(conn -> conn
				.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
				.addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
			);

		return WebClient.builder()
			.baseUrl(baseUrl)
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader("x-api-key", apiKey)
			.defaultHeader("anthropic-version", "2023-06-01")
			.build();
	}
}