package com.qianniuyun.distribution.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AgentQueryService {

    public List<AgentInfo> getAvailableAgents(String skillGroupCode) {
        // 实际实现通过 Redis 或 agent-service API 查询空闲座席
        return new ArrayList<>();
    }

    @Data
    public static class AgentInfo {
        private Long agentId;
        private String agentNo;
        private boolean senior;
        private LocalDateTime idleSince;
        private String skillGroupCode;
    }
}
