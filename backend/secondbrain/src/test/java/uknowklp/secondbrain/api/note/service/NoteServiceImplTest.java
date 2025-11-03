package uknowklp.secondbrain.api.note.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import uknowklp.secondbrain.api.note.domain.Note;
import uknowklp.secondbrain.api.note.dto.NoteRequest;
import uknowklp.secondbrain.api.note.repository.NoteRepository;
import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.api.user.service.UserService;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponseStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoteService 단위 테스트")
class NoteServiceImplTest {

	@InjectMocks
	private NoteServiceImpl noteService;

	@Mock
	private NoteRepository noteRepository;

	@Mock
	private UserService userService;

	private User testUser;
	private NoteRequest validRequest;

	// 각 테스트 실행 전에 공통적으로 필요한 객체를 설정
	@BeforeEach
	void setUp() {
		testUser = User.builder()
			.id(1L)
			.email("test@example.com")
			.name("테스트 사용자")
			.picture("http://example.com/picture.jpg")
			.setAlarm(false)
			.build();

		validRequest = NoteRequest.builder()
			.title("테스트 노트")
			.content("테스트 내용입니다.")
			.build();
	}

	@Test
	@DisplayName("노트 생성 성공 - 정상적인 요청")
	void createNote_Success() {
		// given: 유효한 사용자와 요청 DTO를 준비
		Long userId = 1L;
		Note expectedNote = Note.builder()
			.id(1L)
			.user(testUser)
			.title(validRequest.getTitle())
			.content(validRequest.getContent())
			.remindCount(0)
			.build();

		given(userService.findById(userId)).willReturn(Optional.of(testUser));
		given(noteRepository.save(any(Note.class))).willReturn(expectedNote);

		// when: 노트 생성을 실행
		Note createdNote = noteService.createNote(userId, validRequest);

		// then: 생성된 노트의 정보가 올바른지 확인
		assertNotNull(createdNote);
		assertEquals(validRequest.getTitle(), createdNote.getTitle());
		assertEquals(validRequest.getContent(), createdNote.getContent());
		assertEquals(testUser, createdNote.getUser());
		assertEquals(0, createdNote.getRemindCount());
		assertNull(createdNote.getRemindAt());

		// verify: 메서드 호출 검증
		verify(userService, times(1)).findById(userId);
		verify(noteRepository, times(1)).save(any(Note.class));
	}

	@Test
	@DisplayName("노트 생성 실패 - 존재하지 않는 사용자")
	void createNote_UserNotFound_ShouldThrowException() {
		// given: 존재하지 않는 사용자 ID를 준비
		Long nonExistentUserId = 999L;
		given(userService.findById(nonExistentUserId)).willReturn(Optional.empty());

		// when & then: USER_NOT_FOUND 예외가 발생하는지 확인
		BaseException exception = assertThrows(BaseException.class, () ->
			noteService.createNote(nonExistentUserId, validRequest)
		);
		assertEquals(BaseResponseStatus.USER_NOT_FOUND, exception.getStatus());

		// verify: userService는 호출되었지만, noteRepository.save는 호출되지 않았는지 확인
		verify(userService, times(1)).findById(nonExistentUserId);
		verify(noteRepository, never()).save(any(Note.class));
	}

	@Test
	@DisplayName("노트 생성 성공 - 제목과 내용이 정확히 저장됨")
	void createNote_TitleAndContentSavedCorrectly() {
		// given: 긴 제목과 내용을 가진 요청을 준비
		Long userId = 1L;
		NoteRequest longRequest = NoteRequest.builder()
			.title("이것은 긴 제목을 테스트하기 위한 노트입니다")
			.content("이것은 긴 내용을 테스트하기 위한 노트입니다. 여러 줄의 내용을 담을 수 있습니다.\n두 번째 줄입니다.\n세 번째 줄입니다.")
			.build();

		given(userService.findById(userId)).willReturn(Optional.of(testUser));
		given(noteRepository.save(any(Note.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when: 노트 생성을 실행
		Note createdNote = noteService.createNote(userId, longRequest);

		// then: 제목과 내용이 정확히 저장되었는지 확인
		assertEquals(longRequest.getTitle(), createdNote.getTitle());
		assertEquals(longRequest.getContent(), createdNote.getContent());
	}

	@Test
	@DisplayName("노트 생성 성공 - remindCount가 0으로 초기화됨")
	void createNote_RemindCountInitializedToZero() {
		// given: 유효한 사용자와 요청을 준비
		Long userId = 1L;
		given(userService.findById(userId)).willReturn(Optional.of(testUser));
		given(noteRepository.save(any(Note.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when: 노트 생성을 실행
		Note createdNote = noteService.createNote(userId, validRequest);

		// then: remindCount가 0으로 초기화되고 remindAt은 null인지 확인
		assertEquals(0, createdNote.getRemindCount());
		assertNull(createdNote.getRemindAt());
	}

	@Test
	@DisplayName("노트 생성 성공 - 사용자와 노트의 연관관계가 올바르게 설정됨")
	void createNote_UserRelationshipSetCorrectly() {
		// given: 유효한 사용자와 요청을 준비
		Long userId = 1L;
		given(userService.findById(userId)).willReturn(Optional.of(testUser));
		given(noteRepository.save(any(Note.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when: 노트 생성을 실행
		Note createdNote = noteService.createNote(userId, validRequest);

		// then: User 연관관계가 올바르게 설정되었는지 확인
		assertNotNull(createdNote.getUser());
		assertEquals(userId, createdNote.getUser().getId());
		assertEquals(testUser.getEmail(), createdNote.getUser().getEmail());
		assertEquals(testUser.getName(), createdNote.getUser().getName());
	}

	@Test
	@DisplayName("노트 생성 성공 - 최대 길이 제한에 근접한 입력")
	void createNote_MaxLengthInput() {
		// given: 최대 길이에 근접한 제목과 내용을 준비
		Long userId = 1L;
		String maxTitle = "a".repeat(64); // 64자 제목
		String maxContent = "b".repeat(2048); // 2048자 내용
		NoteRequest maxRequest = NoteRequest.builder()
			.title(maxTitle)
			.content(maxContent)
			.build();

		given(userService.findById(userId)).willReturn(Optional.of(testUser));
		given(noteRepository.save(any(Note.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when: 노트 생성을 실행
		Note createdNote = noteService.createNote(userId, maxRequest);

		// then: 최대 길이의 데이터가 정확히 저장되었는지 확인
		assertEquals(64, createdNote.getTitle().length());
		assertEquals(2048, createdNote.getContent().length());
		assertEquals(maxTitle, createdNote.getTitle());
		assertEquals(maxContent, createdNote.getContent());
	}

	@Test
	@DisplayName("노트 생성 성공 - 여러 사용자가 각각 노트를 생성할 수 있음")
	void createNote_MultipleUsersCanCreateNotes() {
		// given: 두 명의 다른 사용자를 준비
		Long userId1 = 1L;
		Long userId2 = 2L;
		User user2 = User.builder()
			.id(2L)
			.email("user2@example.com")
			.name("사용자2")
			.picture("http://example.com/picture2.jpg")
			.setAlarm(false)
			.build();

		given(userService.findById(userId1)).willReturn(Optional.of(testUser));
		given(userService.findById(userId2)).willReturn(Optional.of(user2));
		given(noteRepository.save(any(Note.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when: 각 사용자가 노트를 생성
		Note note1 = noteService.createNote(userId1, validRequest);
		Note note2 = noteService.createNote(userId2, validRequest);

		// then: 각 노트가 올바른 사용자와 연관되어 있는지 확인
		assertEquals(userId1, note1.getUser().getId());
		assertEquals(userId2, note2.getUser().getId());
		assertNotEquals(note1.getUser().getId(), note2.getUser().getId());

		// verify: 각 사용자에 대해 메서드가 호출되었는지 확인
		verify(userService, times(1)).findById(userId1);
		verify(userService, times(1)).findById(userId2);
		verify(noteRepository, times(2)).save(any(Note.class));
	}

	@Test
	@DisplayName("노트 생성 성공 - 이미지 파일 없이 생성")
	void createNote_WithoutImages_Success() {
		// given: 이미지가 없는 요청
		Long userId = 1L;
		NoteRequest requestWithoutImages = NoteRequest.builder()
			.title("이미지 없는 노트")
			.content("텍스트만 있는 노트입니다.")
			.images(null)
			.build();

		given(userService.findById(userId)).willReturn(Optional.of(testUser));
		given(noteRepository.save(any(Note.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when: 노트 생성
		Note createdNote = noteService.createNote(userId, requestWithoutImages);

		// then: 이미지 마크다운 없이 content가 그대로 저장됨
		assertEquals("텍스트만 있는 노트입니다.", createdNote.getContent());
		verify(noteRepository, times(1)).save(any(Note.class));
	}

	@Test
	@DisplayName("노트 생성 성공 - 이미지 파일 포함 (단일 이미지)")
	void createNote_WithSingleImage_Success() {
		// given: 단일 이미지 파일을 포함한 요청
		Long userId = 1L;
		MockMultipartFile imageFile = new MockMultipartFile(
			"images",
			"test-image.jpg",
			"image/jpeg",
			"test image content".getBytes()
		);
		List<MultipartFile> images = new ArrayList<>();
		images.add(imageFile);

		NoteRequest requestWithImage = NoteRequest.builder()
			.title("이미지 포함 노트")
			.content("이미지가 포함된 노트입니다.")
			.images(images)
			.build();

		given(userService.findById(userId)).willReturn(Optional.of(testUser));
		given(noteRepository.save(any(Note.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when: 노트 생성
		Note createdNote = noteService.createNote(userId, requestWithImage);

		// then: 이미지 마크다운이 content에 추가됨
		assertNotNull(createdNote.getContent());
		assertTrue(createdNote.getContent().contains("이미지가 포함된 노트입니다."));
		assertTrue(createdNote.getContent().contains("![test-image.jpg]"));
		assertTrue(createdNote.getContent().contains("https://placeholder-s3-url/test-image.jpg"));
		verify(noteRepository, times(1)).save(any(Note.class));
	}

	@Test
	@DisplayName("노트 생성 성공 - 이미지 파일 포함 (다중 이미지)")
	void createNote_WithMultipleImages_Success() {
		// given: 다중 이미지 파일을 포함한 요청
		Long userId = 1L;
		MockMultipartFile image1 = new MockMultipartFile(
			"images",
			"image1.jpg",
			"image/jpeg",
			"image1 content".getBytes()
		);
		MockMultipartFile image2 = new MockMultipartFile(
			"images",
			"image2.png",
			"image/png",
			"image2 content".getBytes()
		);
		MockMultipartFile image3 = new MockMultipartFile(
			"images",
			"image3.gif",
			"image/gif",
			"image3 content".getBytes()
		);
		List<MultipartFile> images = new ArrayList<>();
		images.add(image1);
		images.add(image2);
		images.add(image3);

		NoteRequest requestWithImages = NoteRequest.builder()
			.title("다중 이미지 노트")
			.content("여러 이미지가 포함된 노트입니다.")
			.images(images)
			.build();

		given(userService.findById(userId)).willReturn(Optional.of(testUser));
		given(noteRepository.save(any(Note.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when: 노트 생성
		Note createdNote = noteService.createNote(userId, requestWithImages);

		// then: 모든 이미지 마크다운이 content에 추가됨
		assertNotNull(createdNote.getContent());
		assertTrue(createdNote.getContent().contains("여러 이미지가 포함된 노트입니다."));
		assertTrue(createdNote.getContent().contains("![image1.jpg]"));
		assertTrue(createdNote.getContent().contains("![image2.png]"));
		assertTrue(createdNote.getContent().contains("![image3.gif]"));
		assertTrue(createdNote.getContent().contains("https://placeholder-s3-url/image1.jpg"));
		assertTrue(createdNote.getContent().contains("https://placeholder-s3-url/image2.png"));
		assertTrue(createdNote.getContent().contains("https://placeholder-s3-url/image3.gif"));
		verify(noteRepository, times(1)).save(any(Note.class));
	}

	@Test
	@DisplayName("노트 생성 성공 - 빈 이미지 리스트")
	void createNote_WithEmptyImageList_Success() {
		// given: 빈 이미지 리스트
		Long userId = 1L;
		NoteRequest requestWithEmptyList = NoteRequest.builder()
			.title("빈 이미지 리스트 노트")
			.content("이미지 리스트가 비어있습니다.")
			.images(new ArrayList<>())
			.build();

		given(userService.findById(userId)).willReturn(Optional.of(testUser));
		given(noteRepository.save(any(Note.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when: 노트 생성
		Note createdNote = noteService.createNote(userId, requestWithEmptyList);

		// then: content가 그대로 저장됨
		assertEquals("이미지 리스트가 비어있습니다.", createdNote.getContent());
		verify(noteRepository, times(1)).save(any(Note.class));
	}

	@Test
	@DisplayName("노트 생성 실패 - 이미지 추가 후 content 길이 초과")
	void createNote_ContentLengthExceeded_ShouldThrowException() {
		// given: content가 거의 2048자에 가까운 상태에서 이미지 추가 시 초과
		Long userId = 1L;
		String longContent = "a".repeat(2000); // 2000자 content
		MockMultipartFile image = new MockMultipartFile(
			"images",
			"large-image-name-that-will-exceed-limit.jpg",
			"image/jpeg",
			"image content".getBytes()
		);
		List<MultipartFile> images = new ArrayList<>();
		images.add(image);

		NoteRequest requestWithLongContent = NoteRequest.builder()
			.title("길이 초과 노트")
			.content(longContent)
			.images(images)
			.build();

		given(userService.findById(userId)).willReturn(Optional.of(testUser));

		// when & then: BAD_REQUEST 예외가 발생하는지 확인
		BaseException exception = assertThrows(BaseException.class, () ->
			noteService.createNote(userId, requestWithLongContent)
		);
		assertEquals(BaseResponseStatus.BAD_REQUEST, exception.getStatus());

		// verify: noteRepository.save는 호출되지 않았는지 확인
		verify(noteRepository, never()).save(any(Note.class));
	}

	@Test
	@DisplayName("노트 생성 성공 - content 길이가 정확히 2048자")
	void createNote_ContentLengthExactly2048_Success() {
		// given: content가 정확히 2048자
		Long userId = 1L;
		String exactLengthContent = "b".repeat(2048); // 정확히 2048자
		NoteRequest requestWithExactLength = NoteRequest.builder()
			.title("정확한 길이 노트")
			.content(exactLengthContent)
			.images(null)
			.build();

		given(userService.findById(userId)).willReturn(Optional.of(testUser));
		given(noteRepository.save(any(Note.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when: 노트 생성
		Note createdNote = noteService.createNote(userId, requestWithExactLength);

		// then: 정상적으로 저장됨
		assertNotNull(createdNote);
		assertEquals(2048, createdNote.getContent().length());
		verify(noteRepository, times(1)).save(any(Note.class));
	}
}
