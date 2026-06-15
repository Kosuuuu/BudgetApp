package pk.kj.pasir_kosecki_jakub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GroupTransactionDTO {

    @NotNull(message = "ID grupy nie może być puste")
    private Long groupId;

    @NotNull(message = "Kwota nie może być pusta")
    @Positive(message = "Kwota musi być większa od zera")
    private Double amount;

    @NotBlank(message = "Typ transakcji nie może być pusty")
    private String type;

    @NotBlank(message = "Tytuł transakcji nie może być pusty")
    private String title;

    private List<Long> selectedUserIds;
}