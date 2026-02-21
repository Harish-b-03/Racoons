package com.fitnesstracker.config;

import com.fitnesstracker.websocket.GpsWebSocketHandler;
import com.fitnesstracker.websocket.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final GpsWebSocketHandler gpsWebSocketHandler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    private final AppConfig appConfig;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gpsWebSocketHandler, "/ws/gps")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(appConfig.getWebsocket().getAllowedOrigins());
    }
}
