package uknowklp.secondbrain.api.apikey.service;

import uknowklp.secondbrain.api.apikey.dto.ApiKeyResponse;
import uknowklp.secondbrain.api.apikey.dto.ApiKeyValidateResponse;

// API Key 관리 서비스
public interface ApiKeyService {

	// 사용자에게 새로운 API Key 생성 또는 재발급
	ApiKeyResponse generateApiKey(Long userId);

	// API Key 검증 및 userId 반환 (유효하지 않으면 예외 발생)
	ApiKeyValidateResponse validateApiKey(String apiKey);

	// API Key 삭제
	void deleteApiKey(Long userId);
}
