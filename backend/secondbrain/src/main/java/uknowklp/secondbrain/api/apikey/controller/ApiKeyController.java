package uknowklp.secondbrain.api.apikey.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import uknowklp.secondbrain.api.apikey.dto.ApiKeyResponse;
import uknowklp.secondbrain.api.apikey.dto.ApiKeyValidateRequest;
import uknowklp.secondbrain.api.apikey.dto.ApiKeyValidateResponse;
import uknowklp.secondbrain.api.apikey.service.ApiKeyService;
import uknowklp.secondbrain.global.response.BaseResponse;
import uknowklp.secondbrain.global.response.BaseResponseStatus;
import uknowklp.secondbrain.global.security.jwt.dto.CustomUserDetails;

// API Key 관리 컨트롤러
@RestController
@RequestMapping("/api/apikey")
@RequiredArgsConstructor
public class ApiKeyController {

	private final ApiKeyService apiKeyService;

	// API Key 생성 (JWT 인증 필요)
	@PostMapping
	@Operation(summary = "API Key 생성", description = "사용자에게 새로운 API Key를 생성하거나 재발급합니다")
	public ResponseEntity<BaseResponse<ApiKeyResponse>> createApiKey(
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		ApiKeyResponse response = apiKeyService.generateApiKey(userDetails.getUser().getId());
		return ResponseEntity.ok(new BaseResponse<>(response));
	}

	// API Key 검증 및 userId 반환 (공개 엔드포인트)
	@PostMapping("/validate")
	@Operation(summary = "API Key 검증", description = "API Key를 검증하고 해당하는 사용자 ID를 반환합니다")
	public ResponseEntity<BaseResponse<ApiKeyValidateResponse>> validateApiKey(
		@RequestBody ApiKeyValidateRequest request
	) {
		ApiKeyValidateResponse response = apiKeyService.validateApiKey(request.apiKey());
		return ResponseEntity.ok(new BaseResponse<>(response));
	}

	// API Key 삭제 (JWT 인증 필요)
	@DeleteMapping
	@Operation(summary = "API Key 삭제", description = "사용자의 API Key를 삭제합니다")
	public ResponseEntity<BaseResponse<Void>> deleteApiKey(
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		apiKeyService.deleteApiKey(userDetails.getUser().getId());
		return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS));
	}
}
