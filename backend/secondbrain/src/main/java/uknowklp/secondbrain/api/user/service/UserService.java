package uknowklp.secondbrain.api.user.service;

import java.util.Optional;

import uknowklp.secondbrain.api.user.domain.User;

public interface UserService {
	Optional<User> findByEmail(String email);

	Optional<User> findById(Long id);

	User saveOrUpdate(String email, String name, String pictureUrl);
}
