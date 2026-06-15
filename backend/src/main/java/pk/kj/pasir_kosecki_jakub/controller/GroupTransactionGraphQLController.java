package pk.kj.pasir_kosecki_jakub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import pk.kj.pasir_kosecki_jakub.dto.GroupTransactionDTO;
import pk.kj.pasir_kosecki_jakub.service.GroupTransactionService;
import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
public class GroupTransactionGraphQLController {

    private final GroupTransactionService groupTransactionService;

    @MutationMapping
    public Boolean addGroupTransaction(@Argument @Valid GroupTransactionDTO groupTransactionDTO) {
        return groupTransactionService.addGroupTransaction(groupTransactionDTO);
    }
}