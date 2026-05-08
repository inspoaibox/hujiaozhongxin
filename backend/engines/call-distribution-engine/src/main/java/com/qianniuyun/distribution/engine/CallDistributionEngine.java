package com.qianniuyun.distribution.engine;

import com.qianniuyun.common.enums.AgentStatus;
import com.qianniuyun.distribution.model.CallQueueItem;
import com.qianniuyun.distribution.service.AgentQueryService;
import com.qianniuyun.distribution.service.CallQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 呼叫分配引擎
 * 实现技能匹配 + 最长空闲时间分配算法
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CallDistributionEngine {

    private final AgentQueryService agentQueryService;
    private final CallQueueService callQueueService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 分配呼叫给座席
     * 返回分配的座席ID，如果无可用座席则返回null（进入队列）
     */
    public Long distributeCall(String callId, String skillGroupCode, boolean isVip) {
        // 1. 获取技能组可用座席
        List<AgentQueryService.AgentInfo> availableAgents =
                new java.util.ArrayList<>(agentQueryService.getAvailableAgents(skillGroupCode));

        if (availableAgents.isEmpty()) {
            // 无可用座席，加入队列
            callQueueService.enqueue(callId, skillGroupCode, isVip);
            log.info("呼叫进入队列: callId={}, skillGroup={}", callId, skillGroupCode);
            return null;
        }

        // 2. VIP 客户优先分配给高级座席（如果有高级座席则只从高级座席中选）
        if (isVip) {
            List<AgentQueryService.AgentInfo> seniorAgents = availableAgents.stream()
                    .filter(AgentQueryService.AgentInfo::isSenior)
                    .toList();
            if (!seniorAgents.isEmpty()) {
                availableAgents = new java.util.ArrayList<>(seniorAgents);
            }
        }

        // 3. 选择空闲时间最长的座席
        AgentQueryService.AgentInfo selectedAgent = availableAgents.stream()
                .max(Comparator.comparing(AgentQueryService.AgentInfo::getIdleSince))
                .orElse(availableAgents.get(0));

        // 4. 分配呼叫
        assignCallToAgent(callId, selectedAgent.getAgentId());

        log.info("呼叫分配成功: callId={}, agentId={}, agentNo={}",
                callId, selectedAgent.getAgentId(), selectedAgent.getAgentNo());

        return selectedAgent.getAgentId();
    }

    /**
     * 监听座席状态变更事件，当座席变为空闲时触发队列分配
     */
    @KafkaListener(topics = "qianniu.agent.status.events", groupId = "call-distribution")
    public void onAgentStatusChange(Map<String, Object> event) {
        String newStatus = (String) event.get("newStatus");
        if (!AgentStatus.IDLE.name().equals(newStatus)) return;

        Long agentId = Long.valueOf(event.get("agentId").toString());
        String skillGroupCode = (String) event.get("skillGroupCode");

        if (skillGroupCode == null) return;

        // 尝试从队列中取出等待最久的呼叫
        CallQueueItem queueItem = callQueueService.dequeue(skillGroupCode);
        if (queueItem != null) {
            assignCallToAgent(queueItem.getCallId(), agentId);
            log.info("队列呼叫分配: callId={}, agentId={}, waitTime={}s",
                    queueItem.getCallId(), agentId, queueItem.getWaitSeconds());
        }
    }

    /**
     * 监听呼叫创建事件，触发分配
     */
    @KafkaListener(topics = "qianniu.call.events", groupId = "call-distribution")
    public void onCallEvent(Map<String, Object> event) {
        if (!"CALL_CREATED".equals(event.get("eventType"))) return;
        if (!"INBOUND".equals(event.get("callType"))) return;

        String callId = (String) event.get("callId");
        String skillGroupCode = (String) event.getOrDefault("skillGroupCode", "GENERAL");
        boolean isVip = Boolean.TRUE.equals(event.get("isVip"));

        distributeCall(callId, skillGroupCode, isVip);
    }

    /**
     * 执行呼叫分配
     */
    private void assignCallToAgent(String callId, Long agentId) {
        // 发布分配事件，通知呼叫服务和座席服务
        kafkaTemplate.send("qianniu.call.assign", callId,
                Map.of(
                        "callId", callId,
                        "agentId", agentId,
                        "assignedAt", LocalDateTime.now().toString()
                ));
    }
}
