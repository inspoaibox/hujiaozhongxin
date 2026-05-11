package com.qianniuyun.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知服务
 * 处理站内消息、邮件通知
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JavaMailSender mailSender;
    private final NotificationTemplateService templateService;

    /**
     * 监听通知事件
     */
    @KafkaListener(topics = "qianniu.notification.events", groupId = "notification-service")
    public void handleNotificationEvent(Map<String, Object> event) {
        String type = (String) event.get("type");
        if (type == null) {
            type = (String) event.get("eventType");
        }
        if (type == null) {
            log.warn("通知事件缺少 type/eventType: {}", event);
            return;
        }
        log.debug("收到通知事件: type={}", type);

        switch (type) {
            case "TICKET_ASSIGNED" -> notifyTicketAssigned(event);
            case "TICKET_OVERDUE" -> notifyTicketOverdue(event);
            case "INSPECTION_FAILED" -> notifyInspectionFailed(event);
            case "WRAPUP_TIMEOUT" -> notifyWrapupTimeout(event);
            case "QUEUE_ALERT" -> notifyQueueAlert(event);
            default -> log.warn("未知通知类型: {}", type);
        }
    }

    /**
     * 工单分配通知
     */
    private void notifyTicketAssigned(Map<String, Object> event) {
        Long userId = Long.valueOf(event.get("userId").toString());
        String ticketNo = (String) event.get("ticketNo");

        // 推送 WebSocket 站内消息
        pushWebSocketMessage(userId, "TICKET_ASSIGNED",
                Map.of("message", "您有新工单待处理: " + ticketNo, "ticketNo", ticketNo));

        log.info("工单分配通知已发送: userId={}, ticketNo={}", userId, ticketNo);
    }

    /**
     * 工单超时通知
     */
    private void notifyTicketOverdue(Map<String, Object> event) {
        String ticketNo = (String) event.get("ticketNo");

        // 通知管理员（WebSocket + 邮件）
        pushWebSocketMessage(null, "TICKET_OVERDUE",
                Map.of("message", "工单超时未处理: " + ticketNo, "ticketNo", ticketNo));

        log.warn("工单超时通知: ticketNo={}", ticketNo);
    }

    /**
     * 质检不合格通知
     */
    private void notifyInspectionFailed(Map<String, Object> event) {
        String callId = (String) event.get("callId");
        int score = Integer.parseInt(event.get("score").toString());

        pushWebSocketMessage(null, "INSPECTION_FAILED",
                Map.of("message", "通话质检不合格，得分: " + score, "callId", callId));

        log.warn("质检不合格通知: callId={}, score={}", callId, score);
    }

    /**
     * 座席整理超时通知
     */
    private void notifyWrapupTimeout(Map<String, Object> event) {
        Long agentId = Long.valueOf(event.get("agentId").toString());

        pushWebSocketMessage(agentId, "WRAPUP_TIMEOUT",
                Map.of("message", "整理时间已超过3分钟，请及时处理"));
    }

    /**
     * 队列积压预警通知
     */
    private void notifyQueueAlert(Map<String, Object> event) {
        long queueSize = Long.parseLong(event.get("queueSize").toString());
        String skillGroup = (String) event.get("skillGroupCode");

        // 广播给所有管理员
        pushWebSocketMessage(null, "QUEUE_ALERT",
                Map.of("message", "队列积压预警: " + skillGroup + " 队列 " + queueSize + " 个呼叫等待",
                        "queueSize", queueSize));
    }

    /**
     * 发送邮件通知
     */
    public void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[千牛云呼叫中心] " + subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败: to={}", to, e);
        }
    }

    public void sendWebSocketMessage(Long userId, String type, Map<String, Object> data) {
        pushWebSocketMessage(userId, type, data);
    }

    /**
     * 推送 WebSocket 消息
     */
    private void pushWebSocketMessage(Long userId, String type, Map<String, Object> data) {
        Map<String, Object> message = new HashMap<>();
        message.put("userId", userId);
        message.put("type", type);
        message.put("data", data);

        kafkaTemplate.send("qianniu.ws.push",
                userId != null ? String.valueOf(userId) : "BROADCAST",
                message);
    }
}
