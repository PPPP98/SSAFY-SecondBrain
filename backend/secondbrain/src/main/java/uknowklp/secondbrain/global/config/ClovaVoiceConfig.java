package uknowklp.secondbrain.global.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Getter;
import reactor.netty.http.client.HttpClient;

@Configuration
@Getter
public class ClovaVoiceConfig {

	@Value("${clova.voice.client-id}")
	private String clientId;

	@Value("${clova.voice.client-secret}")
	private String clientSecret;

	@Value("${clova.voice.api-url}")
	private String apiUrl;

	// 기본 음성
	@Value("${clova.voice.default-speaker}")
	private String defaultSpeaker;

	// 기본 볼륨 (0 = 보통)
	@Value("${clova.voice.default-volume}")
	private int defaultVolume;

	// 기본 속도 (0 = 보통, -5~5 범위)
	@Value("${clova.voice.default-speed}")
	private int defaultSpeed;

	// 기본 음높이 (0 = 보통, -5~5 범위)
	@Value("${clova.voice.default-pitch}")
	private int defaultPitch;

	// 출력 형식 (mp3 또는 wav)
	@Value("${clova.voice.default-format}")
	private String defaultFormat;

	@Bean
	public WebClient clovaVoiceWebClient() {
		// 네트워크 통신 설정
		HttpClient httpClient = HttpClient.create()
			// 연결 시도 타임아웃
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
			// 응답 대기 타임아웃
			.responseTimeout(Duration.ofSeconds(30))
			.doOnConnected(conn -> conn
				// 데이터 읽기 타임아웃
				.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
				// 데이터 쓰기 타임아웃
				.addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
			);

		return WebClient.builder()
			.baseUrl(apiUrl)  // API URL 설정
			.clientConnector(new ReactorClientHttpConnector(httpClient))  // HttpClient 연결
			// Naver API 인증 헤더 (모든 요청에 자동 포함)
			.defaultHeader("X-NCP-APIGW-API-KEY-ID", clientId)
			.defaultHeader("X-NCP-APIGW-API-KEY", clientSecret)
			.build();
	}
}
