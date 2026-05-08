package com.qianniuyun.agent.repository;

import com.qianniuyun.agent.entity.AgentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentStatusHistoryRepository extends JpaRepository<AgentStatusHistory, Long> {
}
