package pk.kj.pasir_kosecki_jakub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pk.kj.pasir_kosecki_jakub.model.Debt;

import java.util.List;

public interface DebtRepository extends JpaRepository<Debt, Long> {

    List<Debt> findByGroup_Id(Long groupId);

    void deleteByGroup_Id(Long groupId);
}