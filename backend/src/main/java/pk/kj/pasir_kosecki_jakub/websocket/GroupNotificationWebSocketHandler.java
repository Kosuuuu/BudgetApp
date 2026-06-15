package pk.kj.pasir_kosecki_jakub.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pk.kj.pasir_kosecki_jakub.model.User;
import pk.kj.pasir_kosecki_jakub.repository.UserRepository;
import pk.kj.pasir_kosecki_jakub.security.JwtUtil;

import java.net.URI;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class GroupNotificationWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final GroupNotificationSessionRegistry sessionRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session.getUri());

        if (token == null || token.isBlank()) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String email = jwtUtil.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        sessionRegistry.addSession(user.getId(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.removeSession(session);
    }

    private String extractToken(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }

        return Arrays.stream(uri.getQuery().split("&"))
                .filter(parameter -> parameter.startsWith("token="))
                .map(parameter -> parameter.substring("token=".length()))
                .findFirst()
                .orElse(null);
    }
}