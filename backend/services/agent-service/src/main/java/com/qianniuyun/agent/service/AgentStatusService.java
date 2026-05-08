package com.qianniuyun.agent.service;

import com.qianniuyun.agent.entity.Agent;
import com.qianniuyun.agent.entity.AgentStatusHistory;
import com.qianniuyun.agent.event.AgentStatusEvent;
import com.qianniuyun.agent.repository.AgentRepository;
import com.qianniuyun.agent.repository.AgentStatusHistoryRepository;
import com.qianniuyun.common.enums.AgentStatus;
import com.qianniuyun.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 座席状态管理服务
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentStatusService {

    private final AgentRepository agentRepository;
    private final AgentStatusHistoryRepository statusHistoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String AGENT_STATUS_KEY = "agent:status:";
    private static final int WRAPUP_TIMEOUT_SECONDS = 180;

    /**
     * 更新座席状态
     */
    @Transactional
    public void updateStatus(Long agentId, AgentStatus newStatus) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new BusinessException("座席不存在"));

        AgentStatus oldStatus = agent.getStatus();

        // 验证状态转换合法性
        validateStatusTransition(oldStatus, newStatus);

        // 计算上一状态持续时长
        long duration = 0;
        if (agent.getStatusChangedAt() != null) {
            duration = ChronoUnit.SECONDS.between(agent.getStatusChangedAt(), LocalDateTime.now());
        }

        // 记录状态历史
        AgentStatusHistory history = new AgentStatusHistory();
        history.setAgentId(agentId);
        history.setOldStatus(oldStatus != null ? oldStatus.name() : null);
        history.setNewStatus(newStatus.name());
        history.setDuration((int) duration);
        history.setCreatedAt(LocalDateTime.now());
        statusHistoryRepository.save(history);

        // 更新座席状态
        agent.setStatus(newStatus);
        agent.setStatusChangedAt(LocalDateTime.now());
        agentRepository.save(agent);

        // 更新 Redis 缓存（1小时过期）
        String cacheKey = AGENT_STATUS_KEY + agentId;
        redisTemplate.opsForValue().set(cacheKey, newStatus.name(), 1, TimeUnit.HOURS);

        // 发布状态变更事件到 Kafka
        AgentStatusEvent event = AgentStatusEvent.builder()
                .agentId(agentId)
                .agentNo(agent.getAgentNo())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .timestamp(LocalDateTime.now())
                .build();
        kafkaTemplate.send("qianniu.agent.status.events", String.valueOf(agentId), event);

        log.info("座席 {} 状态变更: {} -> {}", agent.getAgentNo(), oldStatus, newStatus);
    }

    /**
     * 座席登录
     */
    @Transactional
    public void agentLogin(Long agentId) {
        updateStatus(agentId, AgentStatus.IDLE);
        log.info("座席 {} 登录", agentId);
    }

    /**
     * 座席登出
     */
    @Transactional
    public void agentLogout(Long agentId) {
        updateStatus(agentId, AgentStatus.OFFLINE);
        // 清除 Redis 缓存
        redisTemplate.delete(AGENT_STATUS_KEY + agentId);
        log.info("座席 {} 登出", agentId);
    }

    /**
     * 获取座席当前状态（优先从 Redis 读取）
     */
    public AgentStatus getStatus(Long agentId) {
        String cached = (String) redisTemplate.opsForValue().get(AGENT_STATUS_KEY + agentId);
        if (cached != null) {
            return AgentStatus.valueOf(cached);
        }
        return agentRepository.findById(agentId)
                .map(Agent::getStatus)
                .orElse(AgentStatus.OFFLINE);
    }

    /**
     * 获取所有空闲座席（用于呼叫分配）
     */
    public List<Agent> getIdleAgents(Long skillGroupId) {
        return agentRepository.findByStatusAndSkillGroupIdAndTrainingModeFalse(
                AgentStatus.IDLE, skillGroupId);
    }

    /**
     * 定时检查整理超时（每30秒执行一次）
     */
    @Scheduled(fixedDelay = 30000)
    public void checkWrapupTimeout() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(WRAPUP_TIMEOUT_SECONDS);
        List<Agent> wrapupAgents = agentRepository
                .findByStatusAndStatusChangedAtBefore(AgentStatus.WRAPUP, threshold);

        for (Agent agent : wrapupAgents) {
            log.warn("座席 {} 整理超时（{}秒），发送提醒", agent.getAgentNo(), WRAPUP_TIMEOUT_SECONDS);
            // 发布超时提醒事件
            AgentStatusEvent event = AgentStatusEvent.builder()
                    .agentId(agent.getId())
                    .agentNo(agent.getAgentNo())
                    .newStatus(AgentStatus.WRAPUP)
                    .eventType("WRAPUP_TIMEOUT")
                    .timestamp(LocalDateTime.now())
                    .build();
            kafkaTemplate.send("qianniu.notification.events", String.valueOf(agent.getId()), event);
        }
    }

    /**
     * 验证状态转换合法性
     */
    private void validateStatusTransition(AgentStatus from, AgentStatus to) {
        if (from == null) return; // 初始状态允许任意转换
        // 离线状态只能转为空闲（登录）
        if (from == AgentStatus.OFFLINE && to != AgentStatus.IDLE) {
            throw new BusinessException("离线状态只能切换为空闲");
        }
        // 通话中不能直接切换为休息
        if (from == AgentStatus.TALKING && to == AgentStatus.REST) {
            throw new BusinessException("通话中不能直接切换为休息状态");
        }
    }
}
