package uknowklp.secondbrain.api.user.service;

import java.util.Optional;

import uknowklp.secondbrain.api.user.domain.User;

public interface UserService {
	Optional<User> findByEmail(String email);

	User saveOrUpdate(String email, String name, String pictureUrl);
}
