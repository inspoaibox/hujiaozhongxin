package com.qianniuyun.agent.repository;

import com.qianniuyun.agent.entity.Agent;
import com.qianniuyun.common.enums.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AgentRepository extends JpaRepository<Agent, Long> {
    List<Agent> findByStatusAndSkillGroupIdAndTrainingModeFalse(AgentStatus status, Long skillGroupId);
    List<Agent> findByStatusAndStatusChangedAtBefore(AgentStatus status, LocalDateTime threshold);
}
