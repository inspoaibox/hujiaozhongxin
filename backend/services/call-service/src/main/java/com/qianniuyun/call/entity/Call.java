package com.qianniuyun.call.entity;

import com.qianniuyun.common.enums.CallStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "calls")
public class Call {
    @Id
    private String id;
    private String callType;

    @Enumerated(EnumType.STRING)
    private CallStatus status;

    private String callerNumber;
    private String calledNumber;
    private Long agentId;
    private Long customerId;
    private Long skillGroupId;
    private LocalDateTime queueEnterAt;
    private LocalDateTime answerAt;
    private LocalDateTime hangupAt;
    private Integer duration;
    private Integer waitDuration;
    private String hangupReason;
    private String summary;
    private Integer satisfaction;
    private String recordingId;
    private LocalDateTime createdAt;
}
