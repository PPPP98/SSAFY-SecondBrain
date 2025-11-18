package uknowklp.secondbrain.api.apikey.dto;

// API Key 검증 요청 DTO
public record ApiKeyValidateRequest(
	String apiKey // 검증할 API Key
) {
}
