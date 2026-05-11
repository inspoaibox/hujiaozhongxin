package com.qianniuyun.ivr.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianniuyun.ivr.freeswitch.FreeSwitchClient;
import com.qianniuyun.ivr.model.IVRFlow;
import com.qianniuyun.ivr.model.IVRNode;
import com.qianniuyun.ivr.model.IVRSession;
import com.qianniuyun.ivr.service.IVRConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * IVR 执行引擎
 * 负责执行 IVR 流程，控制语音播放和按键路由
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IVRExecutionEngine {

    private final IVRConfigService ivrConfigService;
    private final FreeSwitchClient freeSwitchClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String IVR_SESSION_KEY = "ivr:session:";
    private static final int MAX_TIMEOUT_RETRIES = 3;

    /**
     * 启动 IVR 流程
     */
    public void startIVR(String callId, String ivrCode) {
        IVRFlow flow = ivrConfigService.getFlowByCode(ivrCode);
        if (flow == null) {
            log.error("IVR 流程不存在: {}", ivrCode);
            return;
        }

        IVRSession session = new IVRSession();
        session.setCallId(callId);
        session.setFlowId(flow.getId());
        session.setCurrentNodeId(flow.getStartNodeId());
        session.setTimeoutRetries(0);

        saveSession(session);
        executeNode(session, flow.getStartNode());
    }

    /**
     * 处理 DTMF 按键输入
     */
    public void handleDigitInput(String callId, String digit) {
        IVRSession session = getSession(callId);
        if (session == null) {
            log.warn("IVR 会话不存在: callId={}", callId);
            return;
        }

        IVRFlow flow = ivrConfigService.getFlowById(session.getFlowId());
        IVRNode currentNode = flow.getNode(session.getCurrentNodeId());

        if (currentNode == null || !"GET_DIGITS".equals(currentNode.getType())) {
            return;
        }

        // 重置超时计数
        session.setTimeoutRetries(0);

        // 查找匹配的路由
        String nextNodeId = currentNode.getRoutes().entrySet().stream()
                .filter(e -> digit.equals(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(currentNode.getDefaultRoute());

        if (nextNodeId != null) {
            session.setCurrentNodeId(nextNodeId);
            saveSession(session);
            executeNode(session, flow.getNode(nextNodeId));
        }
    }

    /**
     * 处理按键超时
     */
    public void handleTimeout(String callId) {
        IVRSession session = getSession(callId);
        if (session == null) return;

        session.setTimeoutRetries(session.getTimeoutRetries() + 1);

        if (session.getTimeoutRetries() >= MAX_TIMEOUT_RETRIES) {
            // 连续3次超时，转人工
            log.info("IVR 超时次数达到上限，转人工: callId={}", callId);
            transferToAgent(callId, "GENERAL");
            return;
        }

        // 重复当前菜单
        IVRFlow flow = ivrConfigService.getFlowById(session.getFlowId());
        IVRNode currentNode = flow.getNode(session.getCurrentNodeId());
        saveSession(session);
        executeNode(session, currentNode);
    }

    /**
     * 执行 IVR 节点
     */
    private void executeNode(IVRSession session, IVRNode node) {
        if (node == null) {
            log.error("IVR 节点为空: callId={}", session.getCallId());
            return;
        }

        log.debug("执行 IVR 节点: callId={}, nodeId={}, type={}",
                session.getCallId(), node.getNodeId(), node.getType());

        switch (node.getType()) {
            case "PLAY_AUDIO" -> {
                freeSwitchClient.playAudio(session.getCallId(), node.getAudioFile());
                // 播放完成后自动跳转下一节点
                if (node.getNextNodeId() != null) {
                    IVRFlow flow = ivrConfigService.getFlowById(session.getFlowId());
                    session.setCurrentNodeId(node.getNextNodeId());
                    saveSession(session);
                    executeNode(session, flow.getNode(node.getNextNodeId()));
                }
            }
            case "GET_DIGITS" -> {
                // 播放提示语音并等待按键
                if (node.getPromptAudio() != null) {
                    freeSwitchClient.playAudio(session.getCallId(), node.getPromptAudio());
                } else if (node.getPromptText() != null) {
                    freeSwitchClient.playTTS(session.getCallId(), node.getPromptText());
                }
                // 设置超时定时器（10秒）
                scheduleTimeout(session.getCallId(), node.getTimeout() != null ? node.getTimeout() : 10);
            }
            case "TRANSFER" -> transferToAgent(session.getCallId(), node.getSkillGroup());
            case "HANGUP" -> freeSwitchClient.hangup(session.getCallId(), "NORMAL_CLEARING");
            case "PLAY_TTS" -> {
                freeSwitchClient.playTTS(session.getCallId(), node.getText());
                if (node.getNextNodeId() != null) {
                    IVRFlow flow = ivrConfigService.getFlowById(session.getFlowId());
                    session.setCurrentNodeId(node.getNextNodeId());
                    saveSession(session);
                    executeNode(session, flow.getNode(node.getNextNodeId()));
                }
            }
            default -> log.warn("未知 IVR 节点类型: {}", node.getType());
        }
    }

    private void transferToAgent(String callId, String skillGroup) {
        log.info("IVR 转人工: callId={}, skillGroup={}", callId, skillGroup);
        // 通知呼叫分配引擎
        // callDistributionEngine.enqueueCall(callId, skillGroup);
    }

    private void saveSession(IVRSession session) {
        redisTemplate.opsForValue().set(
                IVR_SESSION_KEY + session.getCallId(),
                session,
                30, TimeUnit.MINUTES
        );
    }

    private IVRSession getSession(String callId) {
        Object obj = redisTemplate.opsForValue().get(IVR_SESSION_KEY + callId);
        if (obj instanceof IVRSession) return (IVRSession) obj;
        return null;
    }

    private void scheduleTimeout(String callId, int timeoutSeconds) {
        // 使用 Redis 过期事件或 Quartz 调度超时处理
        // 实际实现中通过 Quartz 定时任务触发 handleTimeout
    }
}
