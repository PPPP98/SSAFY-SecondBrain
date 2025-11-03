package uknowklp.secondbrain.api.user.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.api.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;

	@Override
	public Optional<User> findByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	@Override
	public Optional<User> findById(Long id) {
		return userRepository.findById(id);
	}

	@Override
	public User saveOrUpdate(String email, String name, String pictureUrl) {
		log.debug("saveOrUpdate called - Email: {}, Name: {}, Picture: {}", email, name, pictureUrl);
		Optional<User> existingUser = userRepository.findByEmail(email);

		if (existingUser.isPresent()) {
			// 기존 사용자 업데이트
			User user = existingUser.get();
			log.debug("Updating existing user - Before: name={}, picture={}", user.getName(), user.getPicture());
			user.update(name, pictureUrl);
			User saved = userRepository.save(user);
			log.info("User updated - Email: {}, Name: {}, Picture: {}", email, name, pictureUrl);
			return saved;
		} else {
			// 신규 사용자 생성
			User newUser = User.builder()
				.email(email)
				.name(name)
				.picture(pictureUrl)
				.setAlarm(false)  // 기본값: 알람 Off
				.build();
			User saved = userRepository.save(newUser);
			log.info("New user created - Email: {}, Name: {}, Picture: {}", email, name, pictureUrl);
			return saved;
		}
	}
}
