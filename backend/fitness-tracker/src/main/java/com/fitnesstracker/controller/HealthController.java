package com.fitnesstracker.controller;

import com.fitnesstracker.dto.ApiResponse;
import com.fitnesstracker.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final WebSocketSessionManager sessionManager;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("activeWebSocketConnections", sessionManager.getTotalConnections());
        health.put("activeUsers", sessionManager.getActiveUsers());
        
        return ResponseEntity.ok(ApiResponse.success(health));
    }
}
