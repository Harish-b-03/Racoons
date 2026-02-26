package com.racoons.app.handler;

import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;

@Service
@Log4j2
public class WebSocketMessageHandler extends TextWebSocketHandler {

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        log.info("Received: {} ",payload);

        session.sendMessage(new TextMessage("Server received: " + payload));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("New connection: {}",
                session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        log.info("Connection closed: session -> {}, status -> {}",
                session.getId(), status);
    }
}