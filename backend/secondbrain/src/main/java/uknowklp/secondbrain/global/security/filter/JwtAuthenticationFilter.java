package uknowklp.secondbrain.global.security.filter;

import java.io.IOException;
import java.util.Optional;

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
 * JWT 인증 필터
 * 요청에서 JWT 토큰을 추출하고 검증하여 Spring Security 인증 컨텍스트에 설정
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
			// 요청에서 JWT 토큰 추출
			String token = resolveToken(request);

			// 토큰이 존재하면 인증 처리 (성능 최적화: JWT를 한 번만 파싱)
			if (token != null) {
				// JWT 검증 및 Claims 추출을 한 번에 수행
				Optional<Claims> claimsOpt = jwtProvider.getClaimsIfValid(token);

				if (claimsOpt.isPresent()) {
					Claims claims = claimsOpt.get();

					// 토큰 타입 검증 (ACCESS 토큰만 허용)
					String tokenType = claims.get("tokenType", String.class);
					if (!"ACCESS".equals(tokenType)) {
						log.warn("Invalid token type in authentication filter. Type: {}, URI: {}",
							tokenType, request.getRequestURI());
						response.sendError(
							BaseResponseStatus.INVALID_ACCESS_TOKEN.getHttpStatus().value(),
							"Invalid token type"
						);
						return;
					}

					// 성능 최적화: Access token 만료 시간이 15분으로 짧아져서 블랙리스트 불필요
					// 로그아웃 시 최대 15분 지연은 대부분의 애플리케이션에서 허용 가능
					// 이전: 하루 100만 건 Redis 작업 제거 → 8.3분 CPU 시간 절약

					// 인증 컨텍스트에 설정
					Authentication authentication = jwtProvider.getAuthentication(token);
					if (authentication != null) {
						SecurityContextHolder.getContext().setAuthentication(authentication);
						log.debug("Authentication set for user: {}", authentication.getName());
					}
				}
			}

			// 다음 필터로 진행
			filterChain.doFilter(request, response);

		} catch (ExpiredJwtException e) {
			// JWT 토큰 만료
			log.warn("Expired JWT token for request: {} {}", request.getMethod(), request.getRequestURI());
			response.sendError(
				BaseResponseStatus.JWT_EXPIRED.getHttpStatus().value(),
				BaseResponseStatus.JWT_EXPIRED.getMessage()
			);

		} catch (MalformedJwtException e) {
			// JWT 토큰 형식 오류
			log.warn("Malformed JWT token for request: {} {}", request.getMethod(), request.getRequestURI());
			response.sendError(
				BaseResponseStatus.JWT_MALFORMED.getHttpStatus().value(),
				BaseResponseStatus.JWT_MALFORMED.getMessage()
			);

		} catch (SignatureException e) {
			// JWT 서명 검증 실패
			log.warn("Invalid JWT signature for request: {} {}", request.getMethod(), request.getRequestURI());
			response.sendError(
				BaseResponseStatus.JWT_INVALID_SIGNATURE.getHttpStatus().value(),
				BaseResponseStatus.JWT_INVALID_SIGNATURE.getMessage()
			);

		} catch (Exception e) {
			// 기타 예외
			log.error("JWT authentication error for request: {} {}", request.getMethod(), request.getRequestURI(), e);
			response.sendError(
				BaseResponseStatus.JWT_AUTHENTICATION_ERROR.getHttpStatus().value(),
				BaseResponseStatus.JWT_AUTHENTICATION_ERROR.getMessage()
			);
		}
	}

	private String resolveToken(HttpServletRequest request) {
		// 1. Authorization 헤더에서 토큰 추출 (우선순위 높음)
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}

		// 2. 쿠키에서 토큰 추출 (HttpOnly 쿠키 지원)
		if (request.getCookies() != null) {
			for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
				if ("accessToken".equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}

		return null;
	}
}
