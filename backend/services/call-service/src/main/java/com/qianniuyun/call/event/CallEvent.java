package com.qianniuyun.call.event;

import com.qianniuyun.common.enums.CallStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CallEvent {
    private String callId;
    private String eventType;
    private String callType;
    private CallStatus status;
    private Long agentId;
    private Long customerId;
    private String skillGroupCode;
    private boolean vip;
    private LocalDateTime timestamp;
}
