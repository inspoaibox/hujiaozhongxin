package com.qianniuyun.agent.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "agent_status_history")
public class AgentStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long agentId;
    private String oldStatus;
    private String newStatus;
    private Integer duration;
    private LocalDateTime createdAt;
}
