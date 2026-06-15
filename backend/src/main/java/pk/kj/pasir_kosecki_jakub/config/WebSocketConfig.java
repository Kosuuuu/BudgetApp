package pk.kj.pasir_kosecki_jakub.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import pk.kj.pasir_kosecki_jakub.websocket.GroupNotificationWebSocketHandler;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Profile("!test")
public class WebSocketConfig implements WebSocketConfigurer {

    private final GroupNotificationWebSocketHandler groupNotificationWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(groupNotificationWebSocketHandler, "/ws/group-notifications")
                .setAllowedOriginPatterns("*");
    }
}