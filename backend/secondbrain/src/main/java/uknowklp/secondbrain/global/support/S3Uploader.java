package uknowklp.secondbrain.global.support;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponseStatus;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3Uploader {

	private final S3Client s3Client;

	@Value("${spring.cloud.aws.s3.bucket}")
	private String bucketName;

	@Value("${spring.cloud.aws.region.static}")
	private String region;

	/**
	 * S3에 파일을 업로드하고, 전체 URL을 반환
	 * @param directory 파일을 저장할 디렉토리 이름 (ex: "note-images", "thumbnails")
	 * @param multipartFile 업로드할 파일
	 * @return S3에 저장된 파일의 전체 URL (ex: "https://bucket.s3.region.amazonaws.com/note-images/uuid.jpg")
	 */
	public String upload(String directory, MultipartFile multipartFile) {
		// 파일 존재 여부 검증
		if (multipartFile == null || multipartFile.isEmpty()) {
			throw new BaseException(BaseResponseStatus.EMPTY_FILE);
		}

		// 디렉토리 경로 검증 (보안: 경로 조작 방지)
		if (!StringUtils.hasText(directory) || directory.contains("..")) {
			throw new BaseException(BaseResponseStatus.INVALID_DIRECTORY);
		}

		String originalFileName = multipartFile.getOriginalFilename();
		String extension = "";

		// 확장자 추출
		if (StringUtils.hasText(originalFileName) && originalFileName.contains(".")) {
			extension = originalFileName.substring(originalFileName.lastIndexOf("."));
		}

		// UUID를 사용해 고유한 파일명 생성
		String uniqueFileKey = directory + "/" + UUID.randomUUID().toString() + extension;

		try {
			// S3에 업로드할 요청 객체 생성
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucketName)
				.key(uniqueFileKey)
				.contentType(multipartFile.getContentType())
				.contentLength(multipartFile.getSize())
				.build();

			// S3에 파일 업로드 (스트리밍 방식으로 메모리 효율성 개선)
			s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
				multipartFile.getInputStream(), multipartFile.getSize()));

			// 업로드된 파일의 전체 URL을 직접 구성하여 반환
			return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + uniqueFileKey;

		} catch (IOException e) {
			throw new BaseException(BaseResponseStatus.NOTE_IMAGE_UPLOAD_FAILED);
		}
	}

	/**
	 * S3에 저장된 파일 삭제
	 * @param imageUrl S3 파일의 전체 URL
	 */
	public void delete(String imageUrl) {
		// imageUrl이 비어있거나 null
		if (!StringUtils.hasText(imageUrl)) {
			return;
		}

		try {
			// 전체 URL에서 S3 객체 키(파일 경로)를 추출
			String fileKey = extractKeyFromUrl(imageUrl);

			// S3에 보낼 삭제 요청 객체를 생성
			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
				.bucket(bucketName)
				.key(fileKey)
				.build();

			s3Client.deleteObject(deleteObjectRequest);
			log.info("S3 파일 삭제 성공: {}", fileKey);

		} catch (Exception e) {
			// S3 파일 삭제에 실패하더라도 전체 로직에 영향을 주지 않도록 로그만 남기기
			log.error("S3 파일 삭제에 실패했습니다. imageUrl: {}", imageUrl, e);
		}
	}

	/**
	 * S3 객체 키 추출하는 메서드
	 * @param imageUrl S3 파일의 전체 URL
	 * @return S3 객체 키 (ex: "note-images/uuid.jpg")
	 */
	private String extractKeyFromUrl(String imageUrl) {
		String baseUrl = "https://" + bucketName + ".s3." + region + ".amazonaws.com/";

		// URL 형식 검증
		if (!imageUrl.startsWith(baseUrl)) {
			throw new BaseException(BaseResponseStatus.INVALID_S3_URL);
		}

		String key = imageUrl.substring(baseUrl.length());

		// 키가 비어있는지 검증
		if (key.isEmpty()) {
			throw new BaseException(BaseResponseStatus.INVALID_S3_URL);
		}

		// URL에 포함된 인코딩 문자 디코딩
		return URLDecoder.decode(key, StandardCharsets.UTF_8);
	}
}
