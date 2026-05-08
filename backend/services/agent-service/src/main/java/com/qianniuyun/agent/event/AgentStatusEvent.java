package com.qianniuyun.agent.event;

import com.qianniuyun.common.enums.AgentStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AgentStatusEvent {
    private Long agentId;
    private String agentNo;
    private AgentStatus oldStatus;
    private AgentStatus newStatus;
    private String eventType;
    private LocalDateTime timestamp;
}
