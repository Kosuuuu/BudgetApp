package pk.kj.pasir_kosecki_jakub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pk.kj.pasir_kosecki_jakub.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}