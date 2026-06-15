package pk.kj.pasir_kosecki_jakub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nazwa użytkownika jest wymagana")
    @Column(nullable = false)
    private String username;

    @Email(message = "Adres email jest niepoprawny")
    @NotBlank(message = "Adres email jest wymagany")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Hasło jest wymagane")
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String currency = "PLN";
}
