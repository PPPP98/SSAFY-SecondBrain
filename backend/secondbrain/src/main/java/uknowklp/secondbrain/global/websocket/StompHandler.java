package uknowklp.secondbrain.global.websocket;

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponseStatus;
import uknowklp.secondbrain.global.security.jwt.JwtProvider;
import uknowklp.secondbrain.global.security.jwt.dto.CustomUserDetails;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

	private final JwtProvider jwtProvider;

	// 메시지 전송 전 인터셉트
	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		try {
			if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
				// CONNECT 시 JWT 검증
				String authHeader = accessor.getFirstNativeHeader("Authorization");

				if (authHeader == null || !authHeader.startsWith("Bearer ")) {
					log.warn("WebSocket 연결 실패: Authorization 헤더 없음");
					throw new BaseException(BaseResponseStatus.UNAUTHORIZED);
				}

				String token = authHeader.substring(7);

				// JWT 검증 및 인증 정보 추출
				Authentication authentication = jwtProvider.getAuthentication(token);

				// CustomUserDetails에서 User 정보 추출
				CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
				Long userId = userDetails.getUser().getId();

				// 세션에 사용자 정보 저장 (SUBSCRIBE에서도 사용 가능하도록)
				accessor.setUser(authentication);

				// 세션 속성에 userId 저장 (숫자 ID)
				accessor.getSessionAttributes().put("userId", String.valueOf(userId));

				log.info("WebSocket 연결 성공 - userId: {}, email: {}", userId, userDetails.getUsername());
			}
			else if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
				// SUBSCRIBE 시 본인의 topic만 구독 가능하도록 검증
				String destination = accessor.getDestination();

				// 세션 속성에서 userId 가져오기
				String userId = (String) accessor.getSessionAttributes().get("userId");

				if (userId == null) {
					log.warn("WebSocket SUBSCRIBE 실패: 인증되지 않은 사용자");
					throw new BaseException(BaseResponseStatus.UNAUTHORIZED);
				}

				// /topic/reminder/{userId} 형식 검증
				if (destination != null && destination.startsWith("/topic/reminder/")) {
					String topicUserId = destination.substring("/topic/reminder/".length());

					if (!userId.equals(topicUserId)) {
						log.warn("구독 권한 없음 - userId: {}, destination: {}", userId, destination);
						throw new BaseException(BaseResponseStatus.FORBIDDEN);
					}

					log.info("구독 성공 - userId: {}, destination: {}", userId, destination);
				}
			}
		} catch (BaseException e) {
			// BaseException을 MessagingException으로 변환하여 STOMP ERROR 프레임으로 전달
			log.error("STOMP 인증/인가 실패 - code: {}, message: {}", e.getStatus().getCode(), e.getStatus().getMessage());
			throw new MessagingException(
				String.format("[%d] %s", e.getStatus().getCode(), e.getStatus().getMessage()),
				e
			);
		}

		return message;
	}
}
