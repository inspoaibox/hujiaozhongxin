package com.qianniuyun.call.freeswitch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * FreeSWITCH ESL 客户端（桩实现，待集成真实 ESL 库）
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Component
public class FreeSwitchClient {

    public String originate(String agentExtension, String customerPhone, String callbackApp) {
        log.info("[STUB] originate: agent={}, customer={}", agentExtension, customerPhone);
        return "stub-job-id";
    }

    public void hangup(String callId, String cause) {
        log.info("[STUB] hangup: callId={}, cause={}", callId, cause);
    }

    public void blindTransfer(String callId, String targetExtension) {
        log.info("[STUB] blindTransfer: callId={}, target={}", callId, targetExtension);
    }

    public void attendedTransfer(String callId, String targetExtension) {
        log.info("[STUB] attendedTransfer: callId={}, target={}", callId, targetExtension);
    }

    public void conference(String callId, String conferenceRoom) {
        log.info("[STUB] conference: callId={}, room={}", callId, conferenceRoom);
    }

    public void playAudio(String callId, String audioFile) {
        log.debug("[STUB] playAudio: callId={}, file={}", callId, audioFile);
    }

    public void playTTS(String callId, String text) {
        log.debug("[STUB] playTTS: callId={}", callId);
    }

    public void collectDigits(String callId, int maxDigits, int timeout, Consumer<String> callback) {
        log.debug("[STUB] collectDigits: callId={}", callId);
    }

    public void startRecording(String callId, String recordingPath) {
        log.info("[STUB] startRecording: callId={}, path={}", callId, recordingPath);
    }

    public void stopRecording(String callId, String recordingPath) {
        log.info("[STUB] stopRecording: callId={}", callId);
    }

    public void hold(String callId) {
        log.info("[STUB] hold: callId={}", callId);
    }

    public void unhold(String callId) {
        log.info("[STUB] unhold: callId={}", callId);
    }
}
