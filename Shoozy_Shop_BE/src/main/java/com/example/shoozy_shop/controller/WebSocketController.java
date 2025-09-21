package com.example.shoozy_shop.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    @MessageMapping("/coupon/status")
    @SendTo("/topic/coupon/status")
    public Map<String, Object> handleCouponStatusUpdate(Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Received coupon status update request: {}", message);
        return message;
    }

    @MessageMapping("/admin/connect")
    @SendTo("/topic/admin/connected")
    public Map<String, Object> handleAdminConnection(Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Admin connected with session ID: {}", sessionId);
        
        return Map.of(
            "type", "ADMIN_CONNECTED",
            "sessionId", sessionId,
            "timestamp", System.currentTimeMillis(),
            "message", "Admin connected successfully"
        );
    }

    @GetMapping("/ws/health")
    @ResponseBody
    public Map<String, String> healthCheck() {
        return Map.of("status", "WebSocket service is running");
    }
}

