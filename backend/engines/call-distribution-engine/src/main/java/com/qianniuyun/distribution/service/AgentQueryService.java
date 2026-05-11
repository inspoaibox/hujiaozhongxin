package com.qianniuyun.distribution.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentQueryService {

    private final RedisTemplate<String, Object> redisTemplate;

    public List<AgentInfo> getAvailableAgents(String skillGroupCode) {
        String key = "agent:available:" + skillGroupCode;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            log.debug("没有可用座席缓存: skillGroup={}", skillGroupCode);
            return Collections.emptyList();
        }

        return entries.values().stream()
                .map(this::toAgentInfo)
                .filter(Objects::nonNull)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private AgentInfo toAgentInfo(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }

        AgentInfo info = new AgentInfo();
        info.setAgentId(Long.valueOf(map.get("agentId").toString()));
        info.setAgentNo(Objects.toString(map.get("agentNo"), ""));
        info.setSenior(Boolean.parseBoolean(Objects.toString(map.get("senior"), "false")));
        info.setSkillGroupCode(Objects.toString(map.get("skillGroupCode"), ""));
        String idleSince = Objects.toString(map.get("idleSince"), "");
        info.setIdleSince(idleSince.isBlank() ? LocalDateTime.now() : LocalDateTime.parse(idleSince));
        return info;
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
