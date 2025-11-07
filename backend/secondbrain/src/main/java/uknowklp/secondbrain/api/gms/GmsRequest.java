package uknowklp.secondbrain.api.gms;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GmsRequest(
	String model,
	@JsonProperty("max_tokens")
	Integer maxTokens,
	Double temperature,
	List<GmsMessage> messages
) {
}
