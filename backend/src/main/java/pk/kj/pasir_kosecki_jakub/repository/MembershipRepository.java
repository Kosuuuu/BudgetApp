package pk.kj.pasir_kosecki_jakub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pk.kj.pasir_kosecki_jakub.model.Membership;

import java.util.List;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    List<Membership> findByGroup_Id(Long groupId);

    boolean existsByGroup_IdAndUser_Id(Long groupId, Long userId);

    void deleteByGroup_Id(Long groupId);
}