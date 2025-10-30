package uknowklp.secondbrain.global.security.filter;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import uknowklp.secondbrain.global.response.BaseResponseStatus;
import uknowklp.secondbrain.global.security.jwt.JwtProvider;
import uknowklp.secondbrain.global.security.jwt.service.TokenBlacklistService;

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
	private final TokenBlacklistService blacklistService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		try {
			// 요청에서 JWT 토큰 추출
			String token = resolveToken(request);

			// 토큰이 존재하면 인증 처리
			if (token != null && jwtProvider.validateToken(token)) {
				// Blacklist 확인 (로그아웃되거나 무효화된 토큰 차단)
				String tokenId = jwtProvider.getTokenId(token);
				String tokenType = jwtProvider.getTokenType(token);

				if (blacklistService.isBlacklisted(tokenId, tokenType)) {
					log.warn("Blacklisted token detected. TokenId: {}, Type: {}, URI: {}",
						tokenId, tokenType, request.getRequestURI());
					response.sendError(
						BaseResponseStatus.INVALID_ACCESS_TOKEN.getHttpStatus().value(),
						"Token has been revoked"
					);
					return;
				}

				// 인증 컨텍스트에 설정
				Authentication authentication = jwtProvider.getAuthentication(token);
				if (authentication != null) {
					SecurityContextHolder.getContext().setAuthentication(authentication);
					log.debug("Authentication set for user: {}", authentication.getName());
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
