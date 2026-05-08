package com.qianniuyun.ivr.freeswitch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.function.Consumer;

/**
 * IVR 服务内的 FreeSWITCH 客户端桩
 */
@Slf4j
@Component
public class FreeSwitchClient {
    public void playAudio(String callId, String audioFile) {
        log.debug("[STUB] IVR playAudio: callId={}, file={}", callId, audioFile);
    }
    public void playTTS(String callId, String text) {
        log.debug("[STUB] IVR playTTS: callId={}", callId);
    }
    public void collectDigits(String callId, int maxDigits, int timeout, Consumer<String> callback) {
        log.debug("[STUB] IVR collectDigits: callId={}", callId);
    }
    public void hangup(String callId, String cause) {
        log.info("[STUB] IVR hangup: callId={}, cause={}", callId, cause);
    }
}
