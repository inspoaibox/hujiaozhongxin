package com.qianniuyun.call.freeswitch;

import com.qianniuyun.call.service.CallService;
import com.qianniuyun.common.enums.CallStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.inbound.IEslEventListener;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * FreeSWITCH 事件监听处理器
 * 监听呼叫事件并驱动业务逻辑
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FreeSwitchEventHandler implements IEslEventListener {

    private final Client eslClient;
    private final CallService callService;

    @PostConstruct
    public void init() {
        // 订阅呼叫相关事件
        eslClient.addEventListener(this);
        eslClient.setEventSubscriptions("plain",
                "CHANNEL_CREATE CHANNEL_ANSWER CHANNEL_HANGUP CHANNEL_BRIDGE DTMF RECORD_STOP");
        log.info("FreeSWITCH 事件监听器已注册");
    }

    @Override
    public void eventReceived(EslEvent event) {
        String eventName = event.getEventName();
        String callId = event.getEventHeaders().get("Unique-ID");

        log.debug("收到 FreeSWITCH 事件: {}, callId={}", eventName, callId);

        switch (eventName) {
            case "CHANNEL_CREATE" -> handleChannelCreate(event, callId);
            case "CHANNEL_ANSWER" -> handleChannelAnswer(event, callId);
            case "CHANNEL_HANGUP" -> handleChannelHangup(event, callId);
            case "CHANNEL_BRIDGE" -> handleChannelBridge(event, callId);
            case "DTMF" -> handleDtmf(event, callId);
            case "RECORD_STOP" -> handleRecordStop(event, callId);
            default -> log.trace("忽略事件: {}", eventName);
        }
    }

    @Override
    public void backgroundJobResultReceived(EslEvent event) {
        log.debug("后台任务结果: {}", event.getEventHeaders());
    }

    private void handleChannelCreate(EslEvent event, String callId) {
        String direction = event.getEventHeaders().get("Call-Direction");
        String callerNumber = event.getEventHeaders().get("Caller-Caller-ID-Number");
        String calledNumber = event.getEventHeaders().get("Caller-Destination-Number");

        log.info("呼叫创建: callId={}, direction={}, caller={}, called={}",
                callId, direction, callerNumber, calledNumber);

        if ("inbound".equals(direction)) {
            callService.handleInboundCallCreated(callId, callerNumber, calledNumber);
        }
    }

    private void handleChannelAnswer(EslEvent event, String callId) {
        log.info("呼叫接通: callId={}", callId);
        callService.handleCallAnswered(callId);
    }

    private void handleChannelHangup(EslEvent event, String callId) {
        String hangupCause = event.getEventHeaders().get("Hangup-Cause");
        log.info("呼叫挂断: callId={}, cause={}", callId, hangupCause);
        callService.handleCallHangup(callId, hangupCause);
    }

    private void handleChannelBridge(EslEvent event, String callId) {
        String otherLegId = event.getEventHeaders().get("Other-Leg-Unique-ID");
        log.info("呼叫桥接: callId={}, otherLeg={}", callId, otherLegId);
    }

    private void handleDtmf(EslEvent event, String callId) {
        String digit = event.getEventHeaders().get("DTMF-Digit");
        log.debug("DTMF 按键: callId={}, digit={}", callId, digit);
        callService.handleDtmfInput(callId, digit);
    }

    private void handleRecordStop(EslEvent event, String callId) {
        String recordingPath = event.getEventHeaders().get("Record-File-Path");
        log.info("录音停止: callId={}, path={}", callId, recordingPath);
        callService.handleRecordingStopped(callId, recordingPath);
    }
}
