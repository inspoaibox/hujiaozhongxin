package com.qianniuyun.agent.entity;

import com.qianniuyun.common.enums.AgentStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "agents")
public class Agent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String agentNo;
    private String realName;
    private String extension;
    private Long skillGroupId;
    private String level;

    @Enumerated(EnumType.STRING)
    private AgentStatus status;

    private boolean trainingMode;
    private LocalDateTime statusChangedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
