package pk.kj.pasir_kosecki_jakub.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.kj.pasir_kosecki_jakub.dto.BalanceDto;
import pk.kj.pasir_kosecki_jakub.dto.TransactionDTO;
import pk.kj.pasir_kosecki_jakub.model.Transaction;
import pk.kj.pasir_kosecki_jakub.service.TransactionService;

import java.util.List;

@Controller
public class TransactionGraphQLController {

    private final TransactionService transactionService;

    public TransactionGraphQLController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @QueryMapping
    public List<Transaction> transactions(
            @Argument String type,
            @Argument String tag,
            @Argument Double minAmount,
            @Argument Double maxAmount,
            @Argument String sortBy,
            @Argument String sortDirection
    ) {
        return transactionService.getTransactionsGraphQL(
                type,
                tag,
                minAmount,
                maxAmount,
                sortBy,
                sortDirection
        );
    }

    @QueryMapping
    public BalanceDto userBalance(@Argument Integer days) {
        return transactionService.getUserBalance(days);
    }

    @MutationMapping
    public Transaction addTransaction(@Argument @Valid TransactionDTO transactionDTO) {
        return transactionService.createTransaction(transactionDTO);
    }

    @MutationMapping
    public Transaction updateTransaction(
            @Argument Long id,
            @Argument @Valid TransactionDTO transactionDTO
    ) {
        return transactionService.updateTransaction(id, transactionDTO);
    }

    @MutationMapping
    public Boolean deleteTransaction(@Argument Long id) {
        transactionService.deleteTransaction(id);
        return true;
    }
}