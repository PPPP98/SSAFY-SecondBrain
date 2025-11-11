package uknowklp.secondbrain.api.tts.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;

import reactor.core.publisher.Mono;
import uknowklp.secondbrain.api.tts.dto.TtsRequest;
import uknowklp.secondbrain.api.tts.service.TtsService;

@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {

	private final TtsService ttsService;

	@Operation(summary = "텍스트를 음성으로 변환", description = "Naver Clova Voice를 사용해서 텍스트를 MP3 음성 파일로 변환합니다.")
	@PostMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public Mono<ResponseEntity<byte[]>> convert(@Valid @RequestBody TtsRequest request) {
		// 서비스 호출해서 MP3 바이너리 데이터 받기
		return ttsService.convert(request.text(), request.speaker())
			.map(bytes -> {
				// 응답 헤더 설정 = Content-Type, Content-Length, 파일명
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
				headers.setContentLength(bytes.length);
				headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"speech.mp3\"");

				return ResponseEntity.ok()
					.headers(headers)
					.body(bytes);
			});
	}
}
