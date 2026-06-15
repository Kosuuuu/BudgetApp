package pk.kj.pasir_kosecki_jakub.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pk.kj.pasir_kosecki_jakub.dto.BalanceDto;
import pk.kj.pasir_kosecki_jakub.dto.TransactionDTO;
import pk.kj.pasir_kosecki_jakub.model.Transaction;
import pk.kj.pasir_kosecki_jakub.model.TransactionType;
import pk.kj.pasir_kosecki_jakub.model.User;
import pk.kj.pasir_kosecki_jakub.repository.TransactionRepository;
import pk.kj.pasir_kosecki_jakub.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public List<Transaction> getAllTransactions() {
        User currentUser = getCurrentUser();
        return transactionRepository.findAllByUser(currentUser);
    }

    public Transaction getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID: " + id));

        checkOwnership(transaction);
        return transaction;
    }

    public Transaction createTransaction(TransactionDTO transactionDTO) {
        User currentUser = getCurrentUser();

        Transaction transaction = new Transaction(
                transactionDTO.getAmount(),
                TransactionType.valueOf(transactionDTO.getType()),
                transactionDTO.getTags(),
                transactionDTO.getNotes(),
                currentUser
        );

        transaction.setTimestamp(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    public Transaction updateTransaction(Long id, TransactionDTO transactionDTO) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID: " + id));

        checkOwnership(transaction);

        transaction.setAmount(transactionDTO.getAmount());
        transaction.setType(TransactionType.valueOf(transactionDTO.getType()));
        transaction.setTags(transactionDTO.getTags());
        transaction.setNotes(transactionDTO.getNotes());

        if (transaction.getTimestamp() == null) {
            transaction.setTimestamp(LocalDateTime.now());
        }

        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID: " + id));

        checkOwnership(transaction);
        transactionRepository.delete(transaction);
    }

    public List<Transaction> getTransactionsGraphQL(
            String type,
            String tag,
            Double minAmount,
            Double maxAmount,
            String sortBy,
            String sortDirection
    ) {
        User currentUser = getCurrentUser();

        List<Transaction> transactions = transactionRepository.findByUser(currentUser);

        if (type != null && !type.isBlank()) {
            transactions = transactions.stream()
                    .filter(t -> t.getType() != null)
                    .filter(t -> t.getType().name().equalsIgnoreCase(type))
                    .toList();
        }

        if (tag != null && !tag.isBlank()) {
            transactions = transactions.stream()
                    .filter(t -> t.getTags() != null)
                    .filter(t -> t.getTags().toLowerCase().contains(tag.toLowerCase()))
                    .toList();
        }

        if (minAmount != null) {
            transactions = transactions.stream()
                    .filter(t -> t.getAmount() >= minAmount)
                    .toList();
        }

        if (maxAmount != null) {
            transactions = transactions.stream()
                    .filter(t -> t.getAmount() <= maxAmount)
                    .toList();
        }

        if (sortBy != null && !sortBy.isBlank()) {
            Comparator<Transaction> comparator = switch (sortBy.toLowerCase()) {
                case "amount" -> Comparator.comparing(Transaction::getAmount);
                case "timestamp" -> Comparator.comparing(Transaction::getTimestamp);
                default -> Comparator.comparing(Transaction::getId);
            };

            if ("desc".equalsIgnoreCase(sortDirection)) {
                comparator = comparator.reversed();
            }

            transactions = transactions.stream()
                    .sorted(comparator)
                    .toList();
        }

        return transactions;
    }

    public BalanceDto getUserBalance(Integer days) {
        User currentUser = getCurrentUser();

        List<Transaction> transactions;

        if (days != null && days > 0) {
            LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
            transactions = transactionRepository.findAllByUserAndTimestampGreaterThanEqual(currentUser, fromDate);
        } else {
            transactions = transactionRepository.findByUser(currentUser);
        }

        double totalIncome = transactions.stream()
                .filter(t -> t.getType() != null)
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalExpense = transactions.stream()
                .filter(t -> t.getType() != null)
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double balance = totalIncome - totalExpense;

        return new BalanceDto(totalIncome, totalExpense, balance);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Brak zalogowanego użytkownika");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono zalogowanego użytkownika: " + email));
    }

    private void checkOwnership(Transaction transaction) {
        User currentUser = getCurrentUser();

        if (transaction.getUser() == null || !transaction.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Brak dostępu do tej transakcji");
        }
    }
}