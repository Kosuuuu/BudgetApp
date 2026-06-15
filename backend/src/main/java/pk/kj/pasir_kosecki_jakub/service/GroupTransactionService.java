package pk.kj.pasir_kosecki_jakub.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pk.kj.pasir_kosecki_jakub.dto.GroupNotificationDTO;
import pk.kj.pasir_kosecki_jakub.dto.GroupTransactionDTO;
import pk.kj.pasir_kosecki_jakub.model.Debt;
import pk.kj.pasir_kosecki_jakub.model.Group;
import pk.kj.pasir_kosecki_jakub.model.Membership;
import pk.kj.pasir_kosecki_jakub.model.Transaction;
import pk.kj.pasir_kosecki_jakub.model.TransactionType;
import pk.kj.pasir_kosecki_jakub.model.User;
import pk.kj.pasir_kosecki_jakub.repository.DebtRepository;
import pk.kj.pasir_kosecki_jakub.repository.GroupRepository;
import pk.kj.pasir_kosecki_jakub.repository.MembershipRepository;
import pk.kj.pasir_kosecki_jakub.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GroupTransactionService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;
    private final GroupNotificationService groupNotificationService;

    @Transactional
    public boolean addGroupTransaction(GroupTransactionDTO groupTransactionDTO) {
        User currentUser = currentUserService.getCurrentUser();

        Group group = groupRepository.findById(groupTransactionDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono grupy o ID: " + groupTransactionDTO.getGroupId()
                ));

        if (!membershipRepository.existsByGroup_IdAndUser_Id(group.getId(), currentUser.getId())) {
            throw new RuntimeException("Nie jesteś członkiem tej grupy");
        }

        String type = groupTransactionDTO.getType();

        if (!"EXPENSE".equalsIgnoreCase(type) && !"INCOME".equalsIgnoreCase(type)) {
            throw new RuntimeException("Typ transakcji musi mieć wartość EXPENSE albo INCOME");
        }

        List<Membership> memberships = membershipRepository.findByGroup_Id(group.getId());
        List<Membership> selectedMembers = selectParticipants(groupTransactionDTO, memberships, currentUser);

        if (selectedMembers.size() < 2) {
            throw new RuntimeException("Transakcja grupowa wymaga co najmniej dwóch uczestników");
        }

        double part = BigDecimal.valueOf(groupTransactionDTO.getAmount())
                .divide(BigDecimal.valueOf(selectedMembers.size()), 2, RoundingMode.HALF_UP)
                .doubleValue();

        boolean expense = "EXPENSE".equalsIgnoreCase(type);

        createBalanceTransactionForCurrentUser(groupTransactionDTO, group, currentUser, expense);

        for (Membership membership : selectedMembers) {
            User member = membership.getUser();

            if (member.getId().equals(currentUser.getId())) {
                continue;
            }

            Debt debt = new Debt();
            debt.setGroup(group);
            debt.setAmount(part);
            debt.setTitle(groupTransactionDTO.getTitle());
            debt.setPaidByDebtor(false);
            debt.setConfirmedByCreditor(false);

            if (expense) {
                debt.setDebtor(member);
                debt.setCreditor(currentUser);
            } else {
                debt.setDebtor(currentUser);
                debt.setCreditor(member);
            }

            debtRepository.save(debt);

            if (expense) {
                sendGroupExpenseNotification(
                        group,
                        groupTransactionDTO,
                        currentUser,
                        member,
                        part
                );
            }
        }

        return true;
    }

    private void createBalanceTransactionForCurrentUser(
            GroupTransactionDTO groupTransactionDTO,
            Group group,
            User currentUser,
            boolean expense
    ) {
        Transaction transaction = new Transaction();
        transaction.setAmount(groupTransactionDTO.getAmount());
        transaction.setType(expense ? TransactionType.EXPENSE : TransactionType.INCOME);
        transaction.setTags("group");
        transaction.setNotes(
                "Transakcja grupowa: "
                        + groupTransactionDTO.getTitle()
                        + " | grupa: "
                        + group.getName()
        );
        transaction.setUser(currentUser);
        transaction.setTimestamp(LocalDateTime.now());

        transactionRepository.save(transaction);
    }

    private List<Membership> selectParticipants(
            GroupTransactionDTO groupTransactionDTO,
            List<Membership> memberships,
            User currentUser
    ) {
        List<Long> selectedUserIds = groupTransactionDTO.getSelectedUserIds();

        if (selectedUserIds == null || selectedUserIds.isEmpty()) {
            return memberships;
        }

        Set<Long> uniqueSelectedUserIds = new HashSet<>(selectedUserIds);

        List<Membership> selectedMembers = memberships.stream()
                .filter(membership -> uniqueSelectedUserIds.contains(membership.getUser().getId()))
                .toList();

        if (selectedMembers.size() != uniqueSelectedUserIds.size()) {
            throw new RuntimeException("Wszyscy wybrani użytkownicy muszą być członkami grupy");
        }

        boolean currentUserSelected = selectedMembers.stream()
                .anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));

        if (!currentUserSelected) {
            throw new RuntimeException("Aktualny użytkownik musi być uczestnikiem transakcji grupowej");
        }

        if (selectedMembers.size() < 2) {
            throw new RuntimeException("Transakcja grupowa wymaga co najmniej dwóch uczestników");
        }

        return selectedMembers;
    }

    private void sendGroupExpenseNotification(
            Group group,
            GroupTransactionDTO groupTransactionDTO,
            User currentUser,
            User recipient,
            double userShare
    ) {
        GroupNotificationDTO notification = new GroupNotificationDTO(
                "GROUP_EXPENSE_ADDED",
                group.getId(),
                group.getName(),
                groupTransactionDTO.getTitle(),
                groupTransactionDTO.getAmount(),
                userShare,
                currentUser.getEmail(),
                currentUser.getEmail()
                        + " dodał wydatek \""
                        + groupTransactionDTO.getTitle()
                        + "\" w grupie "
                        + group.getName()
                        + ". Twoja część: "
                        + String.format("%.2f", userShare)
                        + " zł."
        );

        groupNotificationService.sendToUser(recipient.getId(), notification);
    }
}