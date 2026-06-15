package pk.kj.pasir_kosecki_jakub.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InfoController {

    @GetMapping("/api/info")
    public Map<String, String> info() {
        return Map.of(
                "appName", "PASIR Kosecki Jakub",
                "version", "1.0",
                "message", "Witaj w aplikacji stworzonej w Spring Boot!"
        );
    }
}