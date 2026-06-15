package pk.kj.pasir_kosecki_jakub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSettingsDto {

    @NotBlank(message = "Waluta jest wymagana")
    @Pattern(regexp = "PLN|EUR|USD|GBP", message = "Dozwolone waluty: PLN, EUR, USD, GBP")
    private String currency;
}