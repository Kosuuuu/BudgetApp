package pk.kj.pasir_kosecki_jakub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import pk.kj.pasir_kosecki_jakub.dto.GroupNotificationDTO;
import pk.kj.pasir_kosecki_jakub.websocket.GroupNotificationSessionRegistry;

@Service
@RequiredArgsConstructor
public class GroupNotificationService {

    private final GroupNotificationSessionRegistry sessionRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendToUser(Long userId, GroupNotificationDTO notification) {
        String payload;

        try {
            payload = objectMapper.writeValueAsString(notification);
        } catch (Exception e) {
            return;
        }

        for (WebSocketSession session : sessionRegistry.getSessions(userId)) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (Exception ignored) {
                    // Pomijamy zerwane połączenia
                }
            }
        }
    }
}