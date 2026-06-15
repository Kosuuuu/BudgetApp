package pk.kj.pasir_kosecki_jakub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.kj.pasir_kosecki_jakub.dto.DebtDTO;
import pk.kj.pasir_kosecki_jakub.model.Debt;
import pk.kj.pasir_kosecki_jakub.service.DebtService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DebtGraphQLController {

    private final DebtService debtService;

    @QueryMapping
    public List<Debt> groupDebts(@Argument Long groupId) {
        return debtService.getGroupDebts(groupId);
    }

    @MutationMapping
    public Debt createDebt(@Argument @Valid DebtDTO debtDTO) {
        return debtService.createDebt(debtDTO);
    }

    @MutationMapping
    public Boolean deleteDebt(@Argument Long debtId) {
        return debtService.deleteDebt(debtId);
    }

    @MutationMapping
    public Debt markDebtAsPaid(@Argument Long debtId) {
        return debtService.markDebtAsPaid(debtId);
    }

    @MutationMapping
    public Debt confirmDebtPayment(@Argument Long debtId) {
        return debtService.confirmDebtPayment(debtId);
    }
}