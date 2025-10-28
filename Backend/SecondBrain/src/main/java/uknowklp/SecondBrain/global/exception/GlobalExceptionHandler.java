package uknowklp.SecondBrain.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uknowklp.SecondBrain.global.response.BaseResponse;
import uknowklp.SecondBrain.global.response.BaseResponseStatus;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	 // 비즈니스 로직에서 발생하는 모든 커스텀 예외를 처리
	@ExceptionHandler(BaseException.class)
	public ResponseEntity<BaseResponse<Void>> handleBaseException(BaseException e) {

		// BaseException 발생 시, 예외 메시지와 스택 트레이스를 모두 로그로 남김
		log.error("BaseException occurred : {}", e.getMessage(), e);

		BaseResponseStatus status = e.getStatus();
		BaseResponse<Void> responseBody = new BaseResponse<>(status);
		return new ResponseEntity<>(responseBody, status.getHttpStatus());
	}

	// 나머지 예외 처리
	@ExceptionHandler(Exception.class)
	public ResponseEntity<BaseResponse<Void>> handleException(Exception e) {
		log.error("그 외의 예외 처리 : {}", e.getMessage(), e);
		BaseResponse<Void> responseBody = new BaseResponse<>(BaseResponseStatus.SERVER_ERROR);
		return new ResponseEntity<>(responseBody, BaseResponseStatus.SERVER_ERROR.getHttpStatus());
	}
}