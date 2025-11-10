package uknowklp.secondbrain.api.gms.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GmsResponse(
	String id,
	String type,
	String role,
	List<ContentBlock> content,
	String model,
	Usage usage
) {
	// 응답 블록
	public record ContentBlock(
		String type,
		String text
	) {}

	// 토큰 사용량
	public record Usage(
		@JsonProperty("input_tokens")
		Integer inputTokens,
		@JsonProperty("output_tokens")
		Integer outputTokens
	) {}

	// 응답에서 텍스트 추출
	public String extractText() {
		if (content == null || content.isEmpty()) {
			return "";
		}
		return content.get(0).text();
	}
}
