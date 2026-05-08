package com.qianniuyun.wsgateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 座席 WebSocket 处理器 - 实时推送呼叫和状态事件
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    // agentId -> WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String agentId = extractAgentId(session);
        if (agentId != null) {
            sessions.put(agentId, session);
            log.info("座席 {} WebSocket 连接建立, sessionId={}", agentId, session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String agentId = extractAgentId(session);
        if (agentId != null) {
            sessions.remove(agentId);
            log.info("座席 {} WebSocket 连接关闭, status={}", agentId, status);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 处理心跳
        if ("ping".equals(message.getPayload())) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (IOException e) {
                log.warn("发送心跳响应失败", e);
            }
        }
    }

    /**
     * 监听呼叫事件，推送给对应座席
     */
    @KafkaListener(topics = "${kafka.topics.call-events}", groupId = "ws-gateway-call")
    public void onCallEvent(String eventJson) {
        try {
            Map<?, ?> event = objectMapper.readValue(eventJson, Map.class);
            String agentId = (String) event.get("agentId");
            if (agentId != null) {
                pushToAgent(agentId, "CALL_EVENT", event);
            }
        } catch (Exception e) {
            log.error("处理呼叫事件失败", e);
        }
    }

    /**
     * 监听座席状态事件，广播给监控面板
     */
    @KafkaListener(topics = "${kafka.topics.agent-status-events}", groupId = "ws-gateway-agent")
    public void onAgentStatusEvent(String eventJson) {
        try {
            Map<?, ?> event = objectMapper.readValue(eventJson, Map.class);
            // 广播给所有管理员会话
            broadcastToRole("ADMIN", "AGENT_STATUS_EVENT", event);
            broadcastToRole("SUPERVISOR", "AGENT_STATUS_EVENT", event);
        } catch (Exception e) {
            log.error("处理座席状态事件失败", e);
        }
    }

    /**
     * 推送消息给指定座席
     */
    public void pushToAgent(String agentId, String eventType, Object data) {
        WebSocketSession session = sessions.get(agentId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> message = Map.of(
                        "type", eventType,
                        "data", data,
                        "timestamp", System.currentTimeMillis()
                );
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.warn("推送消息给座席 {} 失败", agentId, e);
            }
        }
    }

    /**
     * 广播消息给指定角色的所有在线用户
     */
    public void broadcastToRole(String role, String eventType, Object data) {
        sessions.forEach((agentId, session) -> {
            String sessionRole = (String) session.getAttributes().get("role");
            if (role.equals(sessionRole) && session.isOpen()) {
                try {
                    Map<String, Object> message = Map.of(
                            "type", eventType,
                            "data", data,
                            "timestamp", System.currentTimeMillis()
                    );
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                } catch (IOException e) {
                    log.warn("广播消息失败, agentId={}", agentId, e);
                }
            }
        });
    }

    private String extractAgentId(WebSocketSession session) {
        return (String) session.getAttributes().get("agentId");
    }

    public int getOnlineCount() {
        return (int) sessions.values().stream().filter(WebSocketSession::isOpen).count();
    }
}
