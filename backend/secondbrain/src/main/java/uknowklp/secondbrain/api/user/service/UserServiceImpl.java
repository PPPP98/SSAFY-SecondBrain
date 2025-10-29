package uknowklp.secondbrain.api.user.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uknowklp.secondbrain.api.user.domain.User;
import uknowklp.secondbrain.api.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

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
	public User saveOrUpdate(String email, String name, String pictureUrl) {
		return userRepository.findByEmail(email)
			.map(user -> {
				user.update(name, pictureUrl);
				return user;
			})
			.orElseGet(() -> {
				User newUser = User.builder()
					.email(email)
					.name(name)
					.picture(pictureUrl)
					.setAlarm(false)  // 기본값: 알람 Off
					.build();
				return userRepository.save(newUser);
			});
	}
}
