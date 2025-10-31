package uknowklp.secondbrain.global.security.filter;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import uknowklp.secondbrain.global.response.BaseResponseStatus;
import uknowklp.secondbrain.global.security.jwt.JwtProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 인증 필터 (단순화 버전)
 * 요청에서 JWT 토큰을 추출하고 검증하여 Spring Security 인증 컨텍스트에 설정
 *
 * 개선사항:
 * - 불필요한 블랙리스트 체크 제거
 * - 토큰 검증 로직 단순화
 * - 성능 최적화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtProvider jwtProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		try {
			// 1. 요청에서 JWT 토큰 추출
			String token = resolveToken(request);

			// 2. 토큰이 존재하면 검증 및 인증 처리
			if (token != null) {
				// 3. JWT 토큰 유효성 검증 (서명, 만료 시간 등)
				if (jwtProvider.validateToken(token)) {
					// 4. 검증된 토큰으로 Authentication 객체 생성 및 SecurityContext에 설정
					Authentication authentication = jwtProvider.getAuthentication(token);
					if (authentication != null) {
						SecurityContextHolder.getContext().setAuthentication(authentication);

						if (log.isDebugEnabled()) {
							log.debug("Authentication set for user: {}, URI: {}",
								authentication.getName(), request.getRequestURI());
						}
					}
				}
			}

			// 5. 다음 필터로 진행
			filterChain.doFilter(request, response);

		} catch (ExpiredJwtException e) {
			log.debug("Expired JWT token for request: {}", request.getRequestURI());
			sendUnauthorizedError(response, BaseResponseStatus.JWT_EXPIRED.getMessage());

		} catch (MalformedJwtException e) {
			log.debug("Malformed JWT token for request: {}", request.getRequestURI());
			sendUnauthorizedError(response, BaseResponseStatus.JWT_MALFORMED.getMessage());

		} catch (SignatureException e) {
			log.debug("Invalid JWT signature for request: {}", request.getRequestURI());
			sendUnauthorizedError(response, BaseResponseStatus.JWT_INVALID_SIGNATURE.getMessage());

		} catch (Exception e) {
			log.error("JWT authentication error for request: {}", request.getRequestURI(), e);
			sendUnauthorizedError(response, BaseResponseStatus.JWT_AUTHENTICATION_ERROR.getMessage());
		}
	}

	/**
	 * HTTP 요청에서 JWT 토큰 추출
	 * Authorization 헤더의 Bearer 토큰을 우선 확인
	 *
	 * @param request HTTP 요청
	 * @return JWT 토큰 문자열 또는 null
	 */
	private String resolveToken(HttpServletRequest request) {
		// Authorization 헤더에서 Bearer 토큰 추출
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}

		// 토큰이 없으면 null 반환
		return null;
	}

	/**
	 * 인증 실패 응답 전송
	 *
	 * @param response HTTP 응답
	 * @param message  에러 메시지
	 * @throws IOException IO 예외
	 */
	private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write(
			String.format("{\"status\":401,\"message\":\"%s\"}", message)
		);
	}
}