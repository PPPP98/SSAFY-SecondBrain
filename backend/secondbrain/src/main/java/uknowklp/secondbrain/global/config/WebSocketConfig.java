package uknowklp.secondbrain.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;
import uknowklp.secondbrain.global.websocket.StompHandler;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final StompHandler stompHandler;

	// STOMP 엔드포인트 등록
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")  // WebSocket 연결 엔드포인트
			.setAllowedOriginPatterns("*")  // 개발 단계: 모든 origin 허용
			.withSockJS();  // SockJS fallback 지원
	}

	// 메시지 브로커 설정
	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");  // 클라이언트가 구독할 prefix
		registry.setApplicationDestinationPrefixes("/app");  // 클라이언트가 메시지 전송할 prefix
	}

	// StompHandler 인터셉터 등록
	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompHandler);
	}
}
