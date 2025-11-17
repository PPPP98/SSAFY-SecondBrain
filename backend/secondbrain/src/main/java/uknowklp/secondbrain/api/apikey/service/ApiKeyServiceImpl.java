package uknowklp.secondbrain.api.apikey.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import uknowklp.secondbrain.api.apikey.dto.ApiKeyResponse;
import uknowklp.secondbrain.api.apikey.dto.ApiKeyValidateResponse;
import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.api.user.repository.UserRepository;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponseStatus;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiKeyServiceImpl implements ApiKeyService {

	private final UserRepository userRepository;

	// 사용자에게 새로운 API Key 생성 또는 재발급
	@Override
	@Transactional
	public ApiKeyResponse generateApiKey(Long userId) {
		// 사용자 조회
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

		// UUID 기반 API Key 생성
		String apiKey = UUID.randomUUID().toString();

		// 기존 API Key가 있으면 덮어쓰기 (재발급)
		user.setApiKey(apiKey);

		return new ApiKeyResponse(apiKey);
	}

	// API Key 검증 및 userId 반환
	@Override
	public ApiKeyValidateResponse validateApiKey(String apiKey) {
		// API Key로 사용자 조회
		User user = userRepository.findByApiKey(apiKey)
			.orElseThrow(() -> new BaseException(BaseResponseStatus.INVALID_API_KEY));

		return new ApiKeyValidateResponse(user.getId());
	}

	// API Key 삭제
	@Override
	@Transactional
	public void deleteApiKey(Long userId) {
		// 사용자 조회
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

		// API Key가 없으면 예외 발생
		if (user.getApiKey() == null) {
			throw new BaseException(BaseResponseStatus.API_KEY_NOT_FOUND);
		}

		// API Key 삭제
		user.clearApiKey();
	}
}
