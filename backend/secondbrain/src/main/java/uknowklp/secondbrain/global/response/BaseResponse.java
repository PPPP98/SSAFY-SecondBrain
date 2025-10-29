package uknowklp.secondbrain.global.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.http.HttpStatus;

@JsonPropertyOrder({"success", "code", "message", "data"})
public record BaseResponse<T>(
	@JsonIgnore
	HttpStatus httpStatus,
	boolean success,
	int code,
	String message,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	T data
) {
	// 성공 시 사용할 생성자
	public BaseResponse(T data) {
		this(BaseResponseStatus.SUCCESS.getHttpStatus(),
			BaseResponseStatus.SUCCESS.isSuccess(),
			BaseResponseStatus.SUCCESS.getCode(),
			BaseResponseStatus.SUCCESS.getMessage(),
			data);
	}

	// 실패 시 사용할 생성자
	public BaseResponse(BaseResponseStatus status) {
		this(status.getHttpStatus(),
			status.isSuccess(),
			status.getCode(),
			status.getMessage(),
			null);
	}
}