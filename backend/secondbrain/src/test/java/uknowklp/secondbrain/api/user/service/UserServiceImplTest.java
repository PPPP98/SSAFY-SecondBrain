package uknowklp.secondbrain.api.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.api.user.repository.UserRepository;
import uknowklp.secondbrain.global.exception.BaseException;
import uknowklp.secondbrain.global.response.BaseResponseStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceImplTest {

	@InjectMocks
	private UserServiceImpl userService;

	@Mock
	private UserRepository userRepository;

	private User testUser;

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
	}

	// ========================================
	// toggleSetAlarm 메서드 테스트
	// ========================================

	@Test
	@DisplayName("알람 토글 성공 - false에서 true로 변경")
	void toggleSetAlarm_Success_FalseToTrue() {
		// given: 알람이 꺼져있는 사용자를 준비
		Long userId = 1L;
		User userWithAlarmOff = User.builder()
			.id(userId)
			.email("test@example.com")
			.name("테스트 사용자")
			.picture("http://example.com/picture.jpg")
			.setAlarm(false)  // 초기 상태: Off
			.build();

		given(userRepository.findById(userId)).willReturn(Optional.of(userWithAlarmOff));

		// when: 알람 토글을 실행
		User result = userService.toggleSetAlarm(userId);

		// then: 알람이 true로 변경되었는지 확인
		assertNotNull(result);
		assertTrue(result.isSetAlarm());  // false → true
		assertEquals(userId, result.getId());

		// verify: 메서드 호출 검증
		verify(userRepository, times(1)).findById(userId);
	}

	@Test
	@DisplayName("알람 토글 성공 - true에서 false로 변경")
	void toggleSetAlarm_Success_TrueToFalse() {
		// given: 알람이 켜져있는 사용자를 준비
		Long userId = 1L;
		User userWithAlarmOn = User.builder()
			.id(userId)
			.email("test@example.com")
			.name("테스트 사용자")
			.picture("http://example.com/picture.jpg")
			.setAlarm(true)  // 초기 상태: On
			.build();

		given(userRepository.findById(userId)).willReturn(Optional.of(userWithAlarmOn));

		// when: 알람 토글을 실행
		User result = userService.toggleSetAlarm(userId);

		// then: 알람이 false로 변경되었는지 확인
		assertNotNull(result);
		assertFalse(result.isSetAlarm());  // true → false
		assertEquals(userId, result.getId());

		// verify: 메서드 호출 검증
		verify(userRepository, times(1)).findById(userId);
	}

	@Test
	@DisplayName("알람 토글 성공 - 연속 토글 (false → true → false)")
	void toggleSetAlarm_Success_ConsecutiveToggles() {
		// given: 알람이 꺼져있는 사용자를 준비
		Long userId = 1L;
		User user = User.builder()
			.id(userId)
			.email("test@example.com")
			.name("테스트 사용자")
			.picture("http://example.com/picture.jpg")
			.setAlarm(false)
			.build();

		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		// when: 첫 번째 토글 (false → true)
		User firstToggle = userService.toggleSetAlarm(userId);
		assertTrue(firstToggle.isSetAlarm());

		// when: 두 번째 토글 (true → false)
		User secondToggle = userService.toggleSetAlarm(userId);
		assertFalse(secondToggle.isSetAlarm());

		// verify: findById가 2번 호출되었는지 확인
		verify(userRepository, times(2)).findById(userId);
	}

	@Test
	@DisplayName("알람 토글 실패 - 존재하지 않는 사용자")
	void toggleSetAlarm_UserNotFound_ShouldThrowException() {
		// given: 존재하지 않는 사용자 ID를 준비
		Long nonExistentUserId = 999L;
		given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

		// when & then: USER_NOT_FOUND 예외가 발생하는지 확인
		BaseException exception = assertThrows(BaseException.class, () ->
			userService.toggleSetAlarm(nonExistentUserId)
		);
		assertEquals(BaseResponseStatus.USER_NOT_FOUND, exception.getStatus());

		// verify: userRepository.findById는 호출되었는지 확인
		verify(userRepository, times(1)).findById(nonExistentUserId);
	}

	@Test
	@DisplayName("알람 토글 성공 - 사용자 정보는 유지되고 알람만 변경됨")
	void toggleSetAlarm_Success_OnlyAlarmChanged() {
		// given: 알람이 꺼져있는 사용자를 준비
		Long userId = 1L;
		String originalEmail = "test@example.com";
		String originalName = "테스트 사용자";
		String originalPicture = "http://example.com/picture.jpg";

		User user = User.builder()
			.id(userId)
			.email(originalEmail)
			.name(originalName)
			.picture(originalPicture)
			.setAlarm(false)
			.build();

		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		// when: 알람 토글을 실행
		User result = userService.toggleSetAlarm(userId);

		// then: 알람만 변경되고 다른 정보는 유지됨
		assertNotNull(result);
		assertTrue(result.isSetAlarm());  // 알람만 변경
		assertEquals(userId, result.getId());
		assertEquals(originalEmail, result.getEmail());
		assertEquals(originalName, result.getName());
		assertEquals(originalPicture, result.getPicture());

		// verify: 메서드 호출 검증
		verify(userRepository, times(1)).findById(userId);
	}

	@Test
	@DisplayName("알람 토글 성공 - 여러 사용자가 각각 독립적으로 토글")
	void toggleSetAlarm_Success_MultipleUsersIndependent() {
		// given: 두 명의 다른 사용자를 준비
		Long userId1 = 1L;
		Long userId2 = 2L;

		User user1 = User.builder()
			.id(userId1)
			.email("user1@example.com")
			.name("사용자1")
			.picture("http://example.com/picture1.jpg")
			.setAlarm(false)
			.build();

		User user2 = User.builder()
			.id(userId2)
			.email("user2@example.com")
			.name("사용자2")
			.picture("http://example.com/picture2.jpg")
			.setAlarm(true)
			.build();

		given(userRepository.findById(userId1)).willReturn(Optional.of(user1));
		given(userRepository.findById(userId2)).willReturn(Optional.of(user2));

		// when: 각 사용자가 알람을 토글
		User result1 = userService.toggleSetAlarm(userId1);  // false → true
		User result2 = userService.toggleSetAlarm(userId2);  // true → false

		// then: 각 사용자의 알람이 독립적으로 변경됨
		assertTrue(result1.isSetAlarm());   // user1: false → true
		assertFalse(result2.isSetAlarm());  // user2: true → false
		assertNotEquals(result1.getId(), result2.getId());

		// verify: 각 사용자에 대해 메서드가 호출되었는지 확인
		verify(userRepository, times(1)).findById(userId1);
		verify(userRepository, times(1)).findById(userId2);
	}

	@Test
	@DisplayName("알람 토글 성공 - 반환된 User 객체가 동일한 인스턴스")
	void toggleSetAlarm_Success_ReturnsSameInstance() {
		// given: 알람이 꺼져있는 사용자를 준비
		Long userId = 1L;
		User user = User.builder()
			.id(userId)
			.email("test@example.com")
			.name("테스트 사용자")
			.picture("http://example.com/picture.jpg")
			.setAlarm(false)
			.build();

		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		// when: 알람 토글을 실행
		User result = userService.toggleSetAlarm(userId);

		// then: 반환된 객체가 동일한 User 인스턴스
		assertSame(user, result);
		assertTrue(result.isSetAlarm());

		// verify: 메서드 호출 검증
		verify(userRepository, times(1)).findById(userId);
	}

	@Test
	@DisplayName("알람 토글 성공 - @Transactional에 의한 자동 저장")
	void toggleSetAlarm_Success_TransactionalAutoSave() {
		// given: 알람이 꺼져있는 사용자를 준비
		Long userId = 1L;
		User user = User.builder()
			.id(userId)
			.email("test@example.com")
			.name("테스트 사용자")
			.picture("http://example.com/picture.jpg")
			.setAlarm(false)
			.build();

		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		// when: 알람 토글을 실행
		User result = userService.toggleSetAlarm(userId);

		// then: 알람이 변경되었는지 확인
		assertTrue(result.isSetAlarm());

		// verify: save 메서드가 명시적으로 호출되지 않음 (Dirty Checking)
		verify(userRepository, times(1)).findById(userId);
		verify(userRepository, never()).save(any(User.class));
	}

	// ========================================
	// findById 메서드 테스트
	// ========================================

	@Test
	@DisplayName("사용자 조회 성공 - ID로 사용자 찾기")
	void findById_Success() {
		// given
		Long userId = 1L;
		given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

		// when
		Optional<User> result = userService.findById(userId);

		// then
		assertTrue(result.isPresent());
		assertEquals(testUser.getId(), result.get().getId());
		assertEquals(testUser.getEmail(), result.get().getEmail());

		verify(userRepository, times(1)).findById(userId);
	}

	@Test
	@DisplayName("사용자 조회 실패 - 존재하지 않는 ID")
	void findById_NotFound() {
		// given
		Long nonExistentUserId = 999L;
		given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

		// when
		Optional<User> result = userService.findById(nonExistentUserId);

		// then
		assertFalse(result.isPresent());

		verify(userRepository, times(1)).findById(nonExistentUserId);
	}

	// ========================================
	// findByEmail 메서드 테스트
	// ========================================

	@Test
	@DisplayName("사용자 조회 성공 - 이메일로 사용자 찾기")
	void findByEmail_Success() {
		// given
		String email = "test@example.com";
		given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));

		// when
		Optional<User> result = userService.findByEmail(email);

		// then
		assertTrue(result.isPresent());
		assertEquals(testUser.getEmail(), result.get().getEmail());

		verify(userRepository, times(1)).findByEmail(email);
	}

	@Test
	@DisplayName("사용자 조회 실패 - 존재하지 않는 이메일")
	void findByEmail_NotFound() {
		// given
		String nonExistentEmail = "nonexistent@example.com";
		given(userRepository.findByEmail(nonExistentEmail)).willReturn(Optional.empty());

		// when
		Optional<User> result = userService.findByEmail(nonExistentEmail);

		// then
		assertFalse(result.isPresent());

		verify(userRepository, times(1)).findByEmail(nonExistentEmail);
	}
}
