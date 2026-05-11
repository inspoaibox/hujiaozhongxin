package com.qianniuyun.call.service;

import com.qianniuyun.call.entity.Call;
import com.qianniuyun.call.freeswitch.FreeSwitchClient;
import com.qianniuyun.call.repository.CallRepository;
import com.qianniuyun.common.enums.AgentStatus;
import com.qianniuyun.common.enums.CallStatus;
import com.qianniuyun.common.exception.BusinessException;
import com.qianniuyun.common.model.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 呼叫管理服务 - 核心业务逻辑
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallService {

    private final CallRepository callRepository;
    private final FreeSwitchClient freeSwitchClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 处理呼入呼叫创建（由 FreeSWITCH 事件触发）
     */
    @Transactional
    public Call handleInboundCallCreated(String callId, String callerNumber, String calledNumber) {
        Call call = new Call();
        call.setId(callId);
        call.setCallType("INBOUND");
        call.setStatus(CallStatus.RINGING);
        call.setCallerNumber(callerNumber);
        call.setCalledNumber(calledNumber);
        call.setCreatedAt(LocalDateTime.now());

        callRepository.save(call);

        // 发布呼叫创建事件
        publishCallEvent(call, "CALL_CREATED");

        log.info("呼入呼叫创建: callId={}, caller={}", callId, callerNumber);
        return call;
    }

    /**
     * 发起呼出呼叫
     */
    @Transactional
    public Call createOutboundCall(Long agentId, String customerPhone) {
        if (customerPhone == null || customerPhone.isBlank()) {
            throw new BusinessException("被叫号码不能为空");
        }
        String callId = UUID.randomUUID().toString();

        Call call = new Call();
        call.setId(callId);
        call.setCallType("OUTBOUND");
        call.setStatus(CallStatus.INITIATED);
        call.setCalledNumber(customerPhone);
        call.setAgentId(agentId);
        call.setCreatedAt(LocalDateTime.now());

        callRepository.save(call);

        // 通过 FreeSWITCH 发起呼叫
        freeSwitchClient.originate("agent_" + agentId, customerPhone, "park");

        publishCallEvent(call, "CALL_CREATED");

        log.info("呼出呼叫创建: callId={}, agent={}, customer={}", callId, agentId, customerPhone);
        return call;
    }

    /**
     * 处理呼叫接通
     */
    @Transactional
    public void handleCallAnswered(String callId) {
        Call call = getCallOrThrow(callId);
        call.setStatus(CallStatus.ANSWERED);
        call.setAnswerAt(LocalDateTime.now());
        callRepository.save(call);

        // 启动录音
        String recordingPath = generateRecordingPath(callId);
        freeSwitchClient.startRecording(callId, recordingPath);

        publishCallEvent(call, "CALL_ANSWERED");
        log.info("呼叫接通: callId={}", callId);
    }

    /**
     * 处理呼叫挂断
     */
    @Transactional
    public void handleCallHangup(String callId, String hangupCause) {
        Call call = getCallOrThrow(callId);

        LocalDateTime now = LocalDateTime.now();
        call.setHangupAt(now);
        call.setHangupReason(hangupCause);

        // 计算通话时长
        if (call.getAnswerAt() != null) {
            call.setDuration((int) ChronoUnit.SECONDS.between(call.getAnswerAt(), now));
            call.setStatus(CallStatus.COMPLETED);
        } else if (call.getQueueEnterAt() != null) {
            call.setStatus(CallStatus.ABANDONED);
        } else {
            call.setStatus(CallStatus.FAILED);
        }

        // 计算等待时长
        if (call.getQueueEnterAt() != null && call.getAnswerAt() != null) {
            call.setWaitDuration((int) ChronoUnit.SECONDS.between(
                    call.getQueueEnterAt(), call.getAnswerAt()));
        }

        callRepository.save(call);

        publishCallEvent(call, "CALL_COMPLETED");

        // 如果有座席，更新座席状态为整理
        if (call.getAgentId() != null) {
            publishAgentStatusChange(call.getAgentId(), AgentStatus.WRAPUP);
        }

        log.info("呼叫挂断: callId={}, status={}, duration={}s",
                callId, call.getStatus(), call.getDuration());
    }

    /**
     * 转接呼叫
     */
    @Transactional
    public void transferCall(String callId, Long targetAgentId) {
        Call call = getCallOrThrow(callId);

        if (!CallStatus.ANSWERED.equals(call.getStatus())) {
            throw new BusinessException("只有通话中的呼叫才能转接");
        }

        call.setStatus(CallStatus.TRANSFERRING);
        callRepository.save(call);

        freeSwitchClient.blindTransfer(callId, "agent_" + targetAgentId);

        publishCallEvent(call, "CALL_TRANSFERRED");
        log.info("呼叫转接: callId={}, targetAgent={}", callId, targetAgentId);
    }

    @Transactional
    public void holdCall(String callId) {
        Call call = getCallOrThrow(callId);
        if (!CallStatus.ANSWERED.equals(call.getStatus())) {
            throw new BusinessException("只有通话中的呼叫才能保持");
        }
        call.setStatus(CallStatus.HOLDING);
        callRepository.save(call);
        freeSwitchClient.hold(callId);
        publishCallEvent(call, "CALL_HELD");
    }

    @Transactional
    public void unholdCall(String callId) {
        Call call = getCallOrThrow(callId);
        if (!CallStatus.HOLDING.equals(call.getStatus())) {
            throw new BusinessException("只有保持中的呼叫才能恢复");
        }
        call.setStatus(CallStatus.ANSWERED);
        callRepository.save(call);
        freeSwitchClient.unhold(callId);
        publishCallEvent(call, "CALL_UNHELD");
    }

    /**
     * 发起三方通话
     */
    @Transactional
    public void conferenceCall(String callId, String thirdPartyNumber) {
        Call call = getCallOrThrow(callId);

        if (!CallStatus.ANSWERED.equals(call.getStatus())) {
            throw new BusinessException("只有通话中的呼叫才能发起三方通话");
        }

        call.setStatus(CallStatus.CONFERENCING);
        callRepository.save(call);

        String conferenceRoom = "conf_" + callId;
        freeSwitchClient.conference(callId, conferenceRoom);

        publishCallEvent(call, "CALL_CONFERENCED");
        log.info("三方通话: callId={}, thirdParty={}", callId, thirdPartyNumber);
    }

    /**
     * 处理 DTMF 按键（转发给 IVR 引擎）
     */
    public void handleDtmfInput(String callId, String digit) {
        if (callId == null || digit == null) {
            log.warn("DTMF 事件缺少必要参数: callId={}, digit={}", callId, digit);
            return;
        }
        // 转发给 IVR 服务处理
        kafkaTemplate.send("qianniu.ivr.dtmf", callId,
                java.util.Map.of("callId", callId, "digit", digit));
    }

    /**
     * 处理呼叫分配结果，并推送座席来电弹屏事件。
     */
    @KafkaListener(topics = "qianniu.call.assign", groupId = "call-service")
    @Transactional
    public void handleCallAssigned(Map<String, Object> event) {
        String callId = Objects.toString(event.get("callId"), null);
        if (callId == null) {
            log.warn("呼叫分配事件缺少 callId: {}", event);
            return;
        }

        Call call = callRepository.findById(callId).orElse(null);
        if (call == null) {
            log.warn("呼叫分配事件对应的呼叫不存在: callId={}", callId);
            return;
        }

        Object agentId = event.get("agentId");
        if (agentId != null) {
            call.setAgentId(Long.valueOf(agentId.toString()));
        }
        call.setStatus(CallStatus.RINGING);
        callRepository.save(call);

        publishCallEvent(call, "INCOMING_CALL");
    }

    /**
     * 处理录音停止
     */
    @Transactional
    public void handleRecordingStopped(String callId, String recordingPath) {
        if (callId == null || recordingPath == null) {
            log.warn("录音停止事件缺少必要参数: callId={}, path={}", callId, recordingPath);
            return;
        }
        Call call = callRepository.findById(callId).orElse(null);
        if (call != null) {
            call.setRecordingId(recordingPath);
            callRepository.save(call);
            // 通知录音服务处理文件
            kafkaTemplate.send("qianniu.recording.process", callId,
                    java.util.Map.of("callId", callId, "path", recordingPath));
        }
    }

    /**
     * 更新通话小结
     */
    @Transactional
    public void updateSummary(String callId, String summary) {
        Call call = getCallOrThrow(callId);
        call.setSummary(summary);
        callRepository.save(call);
    }

    public PageResult<Call> queryCalls(int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        Page<Call> result = callRepository.findAll(PageRequest.of(currentPage - 1, currentPageSize));
        return PageResult.of(result.getContent(), result.getTotalElements(), currentPage, currentPageSize);
    }

    public Map<String, Object> getRecordingInfo(String callId) {
        Call call = getCallOrThrow(callId);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("callId", call.getId());
        info.put("recordingId", call.getRecordingId());
        info.put("recordingReady", call.getRecordingId() != null);
        info.put("streamUrl", call.getRecordingId() != null ? "/api/recordings/" + call.getId() + "/stream" : null);
        return info;
    }

    /**
     * 记录满意度评分
     */
    @Transactional
    public void recordSatisfaction(String callId, int score) {
        if (score < 1 || score > 5) {
            throw new BusinessException("满意度评分必须在1-5之间");
        }
        Call call = getCallOrThrow(callId);
        call.setSatisfaction(score);
        callRepository.save(call);
        log.info("满意度评分: callId={}, score={}", callId, score);
    }

    private Call getCallOrThrow(String callId) {
        return callRepository.findById(callId)
                .orElseThrow(() -> new BusinessException("呼叫不存在: " + callId));
    }

    private void publishCallEvent(Call call, String eventType) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("callId", call.getId());
        event.put("eventType", eventType);
        event.put("callType", call.getCallType());
        event.put("status", call.getStatus() != null ? call.getStatus().name() : null);
        event.put("callerNumber", call.getCallerNumber());
        event.put("calledNumber", call.getCalledNumber());
        event.put("agentId", call.getAgentId());
        event.put("customerId", call.getCustomerId());
        event.put("skillGroupCode", resolveSkillGroupCode(call.getSkillGroupId()));
        event.put("vip", false);
        event.put("isVip", false);
        event.put("timestamp", LocalDateTime.now().toString());
        kafkaTemplate.send("qianniu.call.events", call.getId(), event);
    }

    private void publishAgentStatusChange(Long agentId, AgentStatus newStatus) {
        kafkaTemplate.send("qianniu.agent.status.change", String.valueOf(agentId),
                java.util.Map.of("agentId", agentId, "newStatus", newStatus.name()));
    }

    private String generateRecordingPath(String callId) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("/recordings/%d/%02d/%02d/%s.wav",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(), callId);
    }

    private String resolveSkillGroupCode(Long skillGroupId) {
        if (skillGroupId == null) {
            return "GENERAL";
        }
        return switch (skillGroupId.intValue()) {
            case 2 -> "TECH";
            case 3 -> "COMPLAINT";
            case 4 -> "VIP";
            default -> "GENERAL";
        };
    }
}
