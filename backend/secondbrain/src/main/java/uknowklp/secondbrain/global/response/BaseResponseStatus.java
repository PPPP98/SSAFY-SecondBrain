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
	 * -10415 ~ -10420 : Refresh Token 및 인증 코드 에러
	 */
	INVALID_REFRESH_TOKEN(false, HttpStatus.UNAUTHORIZED, -10415, "유효하지 않은 리프레시 토큰입니다."),
	REFRESH_TOKEN_NOT_FOUND(false, HttpStatus.UNAUTHORIZED, -10416, "리프레시 토큰을 찾을 수 없습니다."),
	// REFRESH_TOKEN_EXPIRED(false, HttpStatus.UNAUTHORIZED, -10417, "리프레시 토큰이 만료되었습니다. 다시 로그인해주세요."),
	// REFRESH_TOKEN_REVOKED(false, HttpStatus.UNAUTHORIZED, -10418, "무효화된 리프레시 토큰입니다."),
	// TOKEN_HIJACKING_DETECTED(false, HttpStatus.FORBIDDEN, -10419, "보안 위반이 감지되었습니다. 다시 로그인해주세요."),
	INVALID_AUTHORIZATION_CODE(false, HttpStatus.UNAUTHORIZED, -10420, "유효하지 않거나 만료된 인증 코드입니다."),
	CODE_NOT_PROVIDED(false, HttpStatus.BAD_REQUEST, -10421, "인증 코드가 제공되지 않았습니다."),

	/**
	 * -10100 ~ -10199 : 노트(Note) 관련 에러
	 */
	NOTE_NOT_FOUND(false, HttpStatus.NOT_FOUND, -10101, "존재하지 않는 노트입니다."),
	NOTE_TITLE_EMPTY(false, HttpStatus.BAD_REQUEST, -10102, "노트 제목은 필수입니다."),
	NOTE_TITLE_TOO_LONG(false, HttpStatus.BAD_REQUEST, -10103, "노트 제목은 최대 64자까지 입력 가능합니다."),
	NOTE_CONTENT_EMPTY(false, HttpStatus.BAD_REQUEST, -10104, "노트 내용은 필수입니다."),
	NOTE_CONTENT_TOO_LONG(false, HttpStatus.BAD_REQUEST, -10105, "노트 내용은 최대 2048자까지 입력 가능합니다."),
	NOTE_ACCESS_DENIED(false, HttpStatus.FORBIDDEN, -10106, "해당 노트에 접근 권한이 없습니다."),
	NOTE_INVALID_REQUEST(false, HttpStatus.BAD_REQUEST, -10107, "잘못된 노트 요청입니다."),
	NOTE_IMAGE_UPLOAD_FAILED(false, HttpStatus.INTERNAL_SERVER_ERROR, -10108, "이미지 업로드에 실패했습니다."),
	NOTE_SAVE_FAILED(false, HttpStatus.INTERNAL_SERVER_ERROR, -10109, "노트 저장에 실패했습니다."),

	/**
	 * -10500 : 서버 에러
	 */
	SERVER_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, -10500, "요청 처리 중 서버 오류가 발생했습니다."),
	UNEXPECTED_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, -10503, "예기치 못한 이유로 오류가 발생했습니다."),

	/**
	 * -10600 ~ -10609 : 노트 검색 에러
	 */
	INVALID_SEARCH_KEYWORD(false, HttpStatus.BAD_REQUEST, -10600, "검색 키워드가 유효하지 않습니다."),
	ELASTICSEARCH_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, -10601, "검색 서비스 오류가 발생했습니다."),
	ELASTICSEARCH_CONNECTION_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, -10602, "검색 서버 연결에 실패했습니다."),
	ELASTICSEARCH_INDEX_NOT_FOUND(false, HttpStatus.INTERNAL_SERVER_ERROR, -10603, "검색 인덱스를 찾을 수 없습니다."),
	ELASTICSEARCH_MAPPING_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, -10604, "검색 데이터 형식 오류가 발생했습니다."),

	/**
	 * -10700 : 리마인더 관련 에러
	 */
	REMINDER_DISABLED_BY_USER(false, HttpStatus.BAD_REQUEST, -10700, "전체 리마인더가 비활성화되어 있습니다."),
	REMINDER_ALREADY_ENABLED(false, HttpStatus.BAD_REQUEST, -10701, "이미 리마인더가 활성화되어 있습니다."),
	REMINDER_ALREADY_DISABLED(false, HttpStatus.BAD_REQUEST, -10702, "이미 리마인더가 비활성화되어 있습니다."),
	REMINDER_SCHEDULE_FAILED(false, HttpStatus.INTERNAL_SERVER_ERROR, -10703, "리마인더 예약에 실패했습니다.");

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