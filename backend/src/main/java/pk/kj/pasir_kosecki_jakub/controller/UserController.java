package pk.kj.pasir_kosecki_jakub.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pk.kj.pasir_kosecki_jakub.dto.UserResponseDto;
import pk.kj.pasir_kosecki_jakub.dto.UserSettingsDto;
import pk.kj.pasir_kosecki_jakub.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponseDto getCurrentUser() {
        return userService.getCurrentUser();
    }

    @PutMapping("/settings")
    public UserResponseDto updateSettings(@Valid @RequestBody UserSettingsDto dto) {
        return userService.updateSettings(dto);
    }
}