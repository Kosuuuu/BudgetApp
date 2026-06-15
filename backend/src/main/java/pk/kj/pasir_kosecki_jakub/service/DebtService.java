package pk.kj.pasir_kosecki_jakub.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import pk.kj.pasir_kosecki_jakub.dto.DebtDTO;
import pk.kj.pasir_kosecki_jakub.model.Debt;
import pk.kj.pasir_kosecki_jakub.model.Group;
import pk.kj.pasir_kosecki_jakub.model.User;
import pk.kj.pasir_kosecki_jakub.repository.DebtRepository;
import pk.kj.pasir_kosecki_jakub.repository.GroupRepository;
import pk.kj.pasir_kosecki_jakub.repository.MembershipRepository;
import pk.kj.pasir_kosecki_jakub.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final CurrentUserService currentUserService;

    public List<Debt> getGroupDebts(Long groupId) {
        User currentUser = currentUserService.getCurrentUser();

        if (!membershipRepository.existsByGroup_IdAndUser_Id(groupId, currentUser.getId())) {
            throw new RuntimeException("Nie masz dostępu do długów tej grupy");
        }

        return debtRepository.findByGroup_Id(groupId);
    }

    public Debt createDebt(DebtDTO debtDTO) {
        User currentUser = currentUserService.getCurrentUser();

        Group group = groupRepository.findById(debtDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono grupy o ID: " + debtDTO.getGroupId()
                ));

        if (!membershipRepository.existsByGroup_IdAndUser_Id(group.getId(), currentUser.getId())) {
            throw new RuntimeException("Nie jesteś członkiem tej grupy");
        }

        User debtor = userRepository.findById(debtDTO.getDebtorId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono dłużnika o ID: " + debtDTO.getDebtorId()
                ));

        User creditor = userRepository.findById(debtDTO.getCreditorId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono wierzyciela o ID: " + debtDTO.getCreditorId()
                ));

        if (debtor.getId().equals(creditor.getId())) {
            throw new RuntimeException("Nie można utworzyć długu do samego siebie");
        }

        if (!membershipRepository.existsByGroup_IdAndUser_Id(group.getId(), debtor.getId())) {
            throw new RuntimeException("Dłużnik nie należy do tej grupy");
        }

        if (!membershipRepository.existsByGroup_IdAndUser_Id(group.getId(), creditor.getId())) {
            throw new RuntimeException("Wierzyciel nie należy do tej grupy");
        }

        boolean currentUserIsOwner = group.getOwner().getId().equals(currentUser.getId());
        boolean currentUserIsDebtor = debtor.getId().equals(currentUser.getId());
        boolean currentUserIsCreditor = creditor.getId().equals(currentUser.getId());

        if (!currentUserIsOwner && !currentUserIsDebtor && !currentUserIsCreditor) {
            throw new RuntimeException("Członek grupy może utworzyć dług tylko wtedy, gdy jest jego uczestnikiem");
        }

        Debt debt = new Debt();
        debt.setGroup(group);
        debt.setDebtor(debtor);
        debt.setCreditor(creditor);
        debt.setAmount(debtDTO.getAmount());
        debt.setTitle(debtDTO.getTitle());
        debt.setPaidByDebtor(false);
        debt.setConfirmedByCreditor(false);

        return debtRepository.save(debt);
    }

    public boolean deleteDebt(Long debtId) {
        User currentUser = currentUserService.getCurrentUser();

        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono długu o ID: " + debtId
                ));

        assertCurrentUserCanManageDebt(currentUser, debt);

        debtRepository.delete(debt);

        return true;
    }

    public Debt markDebtAsPaid(Long debtId) {
        User currentUser = currentUserService.getCurrentUser();

        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono długu o ID: " + debtId
                ));

        if (!membershipRepository.existsByGroup_IdAndUser_Id(
                debt.getGroup().getId(),
                currentUser.getId()
        )) {
            throw new AccessDeniedException("Nie jesteś członkiem tej grupy");
        }

        if (!debt.getDebtor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko dłużnik może oznaczyć dług jako opłacony");
        }

        debt.setPaidByDebtor(true);
        debt.setConfirmedByCreditor(false);

        return debtRepository.save(debt);
    }

    public Debt confirmDebtPayment(Long debtId) {
        User currentUser = currentUserService.getCurrentUser();

        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono długu o ID: " + debtId
                ));

        if (!membershipRepository.existsByGroup_IdAndUser_Id(
                debt.getGroup().getId(),
                currentUser.getId()
        )) {
            throw new AccessDeniedException("Nie jesteś członkiem tej grupy");
        }

        if (!debt.getCreditor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko wierzyciel może potwierdzić spłatę długu");
        }

        if (!debt.isPaidByDebtor()) {
            throw new IllegalStateException("Dług musi być najpierw oznaczony jako opłacony przez dłużnika");
        }

        debt.setConfirmedByCreditor(true);

        return debtRepository.save(debt);
    }

    private void assertCurrentUserCanManageDebt(User currentUser, Debt debt) {
        Long currentUserId = currentUser.getId();

        boolean isGroupOwner = debt.getGroup().getOwner().getId().equals(currentUserId);
        boolean isCreditor = debt.getCreditor().getId().equals(currentUserId);
        boolean isDebtor = debt.getDebtor().getId().equals(currentUserId);

        if (!isGroupOwner && !isCreditor && !isDebtor) {
            throw new RuntimeException("Nie masz uprawnień do zarządzania tym długiem");
        }
    }
}