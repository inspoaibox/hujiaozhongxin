package com.qianniuyun.notification.controller;

import com.qianniuyun.common.model.Result;
import com.qianniuyun.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/ws")
    @SuppressWarnings("unchecked")
    public Result<Void> pushWebSocket(@RequestBody Map<String, Object> request) {
        Object userId = request.get("userId");
        Object data = request.get("data");
        notificationService.sendWebSocketMessage(
                userId != null ? Long.valueOf(userId.toString()) : null,
                String.valueOf(request.getOrDefault("type", "MESSAGE")),
                data instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of("message", String.valueOf(data))
        );
        return Result.success();
    }

    @PostMapping("/email")
    public Result<Void> sendEmail(@RequestBody Map<String, String> request) {
        notificationService.sendEmail(request.get("to"), request.get("subject"), request.get("content"));
        return Result.success();
    }
}
