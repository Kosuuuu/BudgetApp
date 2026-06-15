package pk.kj.pasir_kosecki_jakub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pk.kj.pasir_kosecki_jakub.model.User;
import pk.kj.pasir_kosecki_jakub.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Użytkownik nie jest zalogowany");
        }

        String principal = authentication.getName();

        return userRepository.findByEmail(principal)
                .or(() -> userRepository.findByUsername(principal))
                .orElseThrow(() -> new RuntimeException("Nie znaleziono zalogowanego użytkownika: " + principal));
    }
}