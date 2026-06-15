package pk.kj.pasir_kosecki_jakub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BalanceDto {
    private double totalIncome;
    private double totalExpense;
    private double balance;
}