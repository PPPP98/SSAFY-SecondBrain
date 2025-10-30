package uknowklp.secondbrain.global.exception;

import lombok.Getter;
import uknowklp.secondbrain.global.response.BaseResponseStatus;

@Getter
public class BaseException extends RuntimeException {
	private final BaseResponseStatus status;

	public BaseException(BaseResponseStatus status) {
		super(status.getMessage());
		this.status = status;
	}
}