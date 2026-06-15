package pk.kj.pasir_kosecki_jakub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pk.kj.pasir_kosecki_jakub.model.Group;
import pk.kj.pasir_kosecki_jakub.model.User;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByMemberships_User(User user);
}