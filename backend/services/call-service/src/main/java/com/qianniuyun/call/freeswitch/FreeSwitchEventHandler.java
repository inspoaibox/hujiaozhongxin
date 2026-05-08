package com.qianniuyun.call.freeswitch;

import com.qianniuyun.call.service.CallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * FreeSWITCH 事件处理器（桩实现，待集成真实 ESL 库）
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FreeSwitchEventHandler {

    private final CallService callService;

    // 真实实现需要连接 FreeSWITCH ESL，此处为桩
    // 集成时实现 IEslEventListener 接口并订阅事件

    public void simulateInboundCall(String callId, String callerNumber, String calledNumber) {
        log.info("[STUB] 模拟呼入: callId={}, caller={}", callId, callerNumber);
        callService.handleInboundCallCreated(callId, callerNumber, calledNumber);
    }
}
