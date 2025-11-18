package uknowklp.secondbrain.api.apikey.dto;

// API Key 생성 응답 DTO
public record ApiKeyResponse(
	String apiKey // 생성된 API Key (UUID 형식)
) {
}
