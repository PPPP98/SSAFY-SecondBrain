package uknowklp.secondbrain.api.gms.dto;

public record GmsMessage(
	String role,
	String content
) {
	// 사용자 메시지 생성 헬퍼
	public static GmsMessage user(String content) {
		return new GmsMessage("user",  content);
	}
}
