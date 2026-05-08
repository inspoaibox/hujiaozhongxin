package com.qianniuyun.call.freeswitch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.transport.message.EslMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * FreeSWITCH ESL 客户端
 * 负责与 FreeSWITCH 服务器通信，控制呼叫
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FreeSwitchClient {

    private final Client eslClient;

    @Value("${freeswitch.host:localhost}")
    private String host;

    @Value("${freeswitch.port:8021}")
    private int port;

    /**
     * 发起呼出呼叫
     */
    public String originate(String agentExtension, String customerPhone, String callbackApp) {
        String command = String.format(
                "originate {origination_caller_id_number=%s}sofia/gateway/pstn/%s &%s",
                agentExtension, customerPhone, callbackApp
        );
        EslMessage response = eslClient.sendSyncApiCommand("bgapi", command);
        String jobId = extractJobId(response);
        log.info("发起呼出: agent={}, customer={}, jobId={}", agentExtension, customerPhone, jobId);
        return jobId;
    }

    /**
     * 挂断呼叫
     */
    public void hangup(String callId, String cause) {
        String command = String.format("uuid_kill %s %s", callId, cause != null ? cause : "NORMAL_CLEARING");
        eslClient.sendSyncApiCommand("api", command);
        log.info("挂断呼叫: callId={}, cause={}", callId, cause);
    }

    /**
     * 转接呼叫（盲转）
     */
    public void blindTransfer(String callId, String targetExtension) {
        String command = String.format("uuid_transfer %s %s", callId, targetExtension);
        eslClient.sendSyncApiCommand("api", command);
        log.info("盲转呼叫: callId={}, target={}", callId, targetExtension);
    }

    /**
     * 转接呼叫（协商转）
     */
    public void attendedTransfer(String callId, String targetExtension) {
        // 先桥接到目标，确认后再转接
        String command = String.format("uuid_bridge %s %s", callId, targetExtension);
        eslClient.sendSyncApiCommand("api", command);
        log.info("协商转呼叫: callId={}, target={}", callId, targetExtension);
    }

    /**
     * 发起三方通话
     */
    public void conference(String callId, String conferenceRoom) {
        String command = String.format("uuid_transfer %s conference:%s", callId, conferenceRoom);
        eslClient.sendSyncApiCommand("api", command);
        log.info("发起三方通话: callId={}, room={}", callId, conferenceRoom);
    }

    /**
     * 播放语音文件
     */
    public void playAudio(String callId, String audioFile) {
        String command = String.format("uuid_broadcast %s %s aleg", callId, audioFile);
        eslClient.sendSyncApiCommand("api", command);
        log.debug("播放语音: callId={}, file={}", callId, audioFile);
    }

    /**
     * 播放 TTS 文字转语音
     */
    public void playTTS(String callId, String text) {
        String command = String.format(
                "uuid_broadcast %s say::zh-CN/female/say_this_string/%s aleg",
                callId, text
        );
        eslClient.sendSyncApiCommand("api", command);
    }

    /**
     * 收集 DTMF 按键
     */
    public void collectDigits(String callId, int maxDigits, int timeout,
                               Consumer<String> callback) {
        String command = String.format(
                "uuid_play_and_get_digits %s 1 %d %d 3000 # silence_stream://250 digits \\d",
                callId, maxDigits, timeout * 1000
        );
        eslClient.sendSyncApiCommand("api", command);
    }

    /**
     * 开始录音
     */
    public void startRecording(String callId, String recordingPath) {
        String command = String.format("uuid_record %s start %s", callId, recordingPath);
        eslClient.sendSyncApiCommand("api", command);
        log.info("开始录音: callId={}, path={}", callId, recordingPath);
    }

    /**
     * 停止录音
     */
    public void stopRecording(String callId, String recordingPath) {
        String command = String.format("uuid_record %s stop %s", callId, recordingPath);
        eslClient.sendSyncApiCommand("api", command);
        log.info("停止录音: callId={}", callId);
    }

    /**
     * 保持通话
     */
    public void hold(String callId) {
        String command = String.format("uuid_hold %s", callId);
        eslClient.sendSyncApiCommand("api", command);
    }

    /**
     * 恢复通话
     */
    public void unhold(String callId) {
        String command = String.format("uuid_hold off %s", callId);
        eslClient.sendSyncApiCommand("api", command);
    }

    private String extractJobId(EslMessage response) {
        if (response != null && response.getBodyLines() != null) {
            for (String line : response.getBodyLines()) {
                if (line.startsWith("+OK Job-UUID:")) {
                    return line.substring("+OK Job-UUID:".length()).trim();
                }
            }
        }
        return null;
    }
}
