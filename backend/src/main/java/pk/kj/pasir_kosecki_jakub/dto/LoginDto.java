package pk.kj.pasir_kosecki_jakub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginDto {

    @NotBlank(message = "Adres email jest wymagany")
    @Email(message = "Adres email jest niepoprawny")
    private String email;

    @NotBlank(message = "Hasło jest wymagane")
    private String password;
}
