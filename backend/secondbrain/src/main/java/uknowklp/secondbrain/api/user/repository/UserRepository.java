package uknowklp.secondbrain.api.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import uknowklp.secondbrain.api.user.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);

	// API Key로 사용자 조회 (MCP 연동용)
	Optional<User> findByApiKey(String apiKey);
}
