package pk.kj.pasir_kosecki_jakub.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GroupNotificationSessionRegistry {

    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();

    public void addSession(Long userId, WebSocketSession session) {
        sessionsByUserId
                .computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    public void removeSession(WebSocketSession session) {
        sessionsByUserId.values().forEach(sessions -> sessions.remove(session));
    }

    public Set<WebSocketSession> getSessions(Long userId) {
        return sessionsByUserId.getOrDefault(userId, Collections.emptySet());
    }
}