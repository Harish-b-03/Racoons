package com.fitnesstracker.websocket;

import com.fitnesstracker.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final AppConfig appConfig;
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public void addSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>());
        
        if (sessions.size() >= appConfig.getWebsocket().getMaxConnectionsPerUser()) {
            WebSocketSession oldestSession = sessions.iterator().next();
            removeSession(userId, oldestSession);
            log.info("Removed oldest session for user {} due to max connections limit", userId);
        }
        
        sessions.add(session);
        log.debug("Added session {} for user {}. Total sessions: {}", session.getId(), userId, sessions.size());
    }

    public void removeSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
            log.debug("Removed session {} for user {}. Remaining sessions: {}", 
                session.getId(), userId, sessions.size());
        }
    }

    public Set<WebSocketSession> getUserSessions(Long userId) {
        return userSessions.getOrDefault(userId, Set.of());
    }

    public boolean hasActiveSessions(Long userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public int getTotalConnections() {
        return userSessions.values().stream()
            .mapToInt(Set::size)
            .sum();
    }

    public int getActiveUsers() {
        return userSessions.size();
    }
}
