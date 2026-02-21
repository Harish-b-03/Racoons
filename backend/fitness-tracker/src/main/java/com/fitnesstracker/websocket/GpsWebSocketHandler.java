package com.fitnesstracker.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitnesstracker.dto.GpsDataDto;
import com.fitnesstracker.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GpsWebSocketHandler extends TextWebSocketHandler {

    private final LocationService locationService;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            sessionManager.addSession(userId, session);
            log.info("WebSocket connection established for user: {}, session: {}", userId, session.getId());
            sendMessage(session, new WebSocketMessage("CONNECTED", "Successfully connected to GPS tracking"));
        } else {
            log.warn("Connection rejected - no user ID in session: {}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            log.warn("Received message from unauthenticated session: {}", session.getId());
            return;
        }

        try {
            String payload = message.getPayload();
            log.debug("Received GPS data from user {}: {}", userId, payload);

            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);

            switch (wsMessage.getType()) {
                case "GPS_DATA" -> handleGpsData(userId, session, wsMessage);
                case "PING" -> handlePing(session);
                case "START_ACTIVITY" -> handleStartActivity(userId, session, wsMessage);
                case "STOP_ACTIVITY" -> handleStopActivity(userId, session, wsMessage);
                default -> log.warn("Unknown message type: {}", wsMessage.getType());
            }

        } catch (Exception e) {
            log.error("Error processing message from user {}: {}", userId, e.getMessage(), e);
            sendError(session, "Error processing GPS data: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            sessionManager.removeSession(userId, session);
            log.info("WebSocket connection closed for user: {}, session: {}, status: {}", 
                userId, session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = getUserIdFromSession(session);
        log.error("WebSocket transport error for user {}, session {}: {}", 
            userId, session.getId(), exception.getMessage(), exception);
        
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void handleGpsData(Long userId, WebSocketSession session, WebSocketMessage message) {
        try {
            GpsDataDto gpsData = objectMapper.convertValue(message.getData(), GpsDataDto.class);
            gpsData.setUserId(userId);
            
            locationService.updateUserLocation(gpsData);
            
            sendMessage(session, new WebSocketMessage("GPS_ACK", "Location updated successfully"));
            
        } catch (Exception e) {
            log.error("Error handling GPS data for user {}: {}", userId, e.getMessage(), e);
            sendError(session, "Failed to update location");
        }
    }

    private void handlePing(WebSocketSession session) {
        sendMessage(session, new WebSocketMessage("PONG", "Heartbeat acknowledged"));
    }

    private void handleStartActivity(Long userId, WebSocketSession session, WebSocketMessage message) {
        log.info("Start activity request from user: {}", userId);
        sendMessage(session, new WebSocketMessage("ACTIVITY_STARTED", "Activity tracking started"));
    }

    private void handleStopActivity(Long userId, WebSocketSession session, WebSocketMessage message) {
        log.info("Stop activity request from user: {}", userId);
        sendMessage(session, new WebSocketMessage("ACTIVITY_STOPPED", "Activity tracking stopped"));
    }

    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("Error sending message to session {}: {}", session.getId(), e.getMessage(), e);
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        sendMessage(session, new WebSocketMessage("ERROR", errorMessage));
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        Object userId = attributes.get("userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }
}
