package uknowklp.secondbrain.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BaseResponseStatus {

	/**
	 * 200 : 요청 성공
	 */
	SUCCESS(true, HttpStatus.OK, 200, "요청에 성공했습니다."),
	CREATED(true, HttpStatus.CREATED, 201, "생성에 성공했습니다."),

	/**
	 * -10400 : 사용자 요청 에러
	 */
	BAD_REQUEST(false, HttpStatus.BAD_REQUEST, -10400, "잘못된 요청입니다."),
	INVALID_ACCESS_TOKEN(false, HttpStatus.UNAUTHORIZED, -10401, "유효하지 않은 액세스 토큰입니다."),
	USER_NOT_FOUND(false, HttpStatus.NOT_FOUND, -10402, "존재하지 않는 사용자입니다."),
	NEED_LOGIN(false, HttpStatus.FORBIDDEN, -10403, "로그인이 필요한 서비스입니다."),
	EMPTY_FILE(false, HttpStatus.BAD_REQUEST, -10404, "파일이 존재하지 않습니다."),

	/**
	 * -10405 ~ -10409 : OAuth2 인증 에러
	 */
	OAUTH_ACCESS_DENIED(false, HttpStatus.UNAUTHORIZED, -10405, "로그인이 취소되었습니다. 다시 시도해주세요."),
	OAUTH_SERVER_ERROR(false, HttpStatus.SERVICE_UNAVAILABLE, -10406, "인증 서버 연결에 실패했습니다. 잠시 후 다시 시도해주세요."),
	OAUTH_UNKNOWN_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, -10407, "로그인 중 오류가 발생했습니다. 다시 시도해주세요."),

	/**
	 * -10411 ~ -10414 : JWT 토큰 에러
	 */
	JWT_EXPIRED(false, HttpStatus.UNAUTHORIZED, -10411, "토큰이 만료되었습니다. 다시 로그인해주세요."),
	JWT_MALFORMED(false, HttpStatus.UNAUTHORIZED, -10412, "유효하지 않은 토큰입니다."),
	JWT_INVALID_SIGNATURE(false, HttpStatus.UNAUTHORIZED, -10413, "토큰 서명이 유효하지 않습니다."),
	JWT_AUTHENTICATION_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, -10414, "인증 처리 중 오류가 발생했습니다."),

	/**
	 * -10500 : 서버 에러
	 */
	SERVER_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, -10500, "요청 처리 중 서버 오류가 발생했습니다."),
	UNEXPECTED_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, -10503, "예기치 못한 이유로 오류가 발생했습니다.");


	private final boolean isSuccess;
	private final HttpStatus httpStatus;
	private final int code;
	private final String message;

	BaseResponseStatus(boolean isSuccess, HttpStatus httpStatus, int code, String message) {
		this.isSuccess = isSuccess;
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}
}