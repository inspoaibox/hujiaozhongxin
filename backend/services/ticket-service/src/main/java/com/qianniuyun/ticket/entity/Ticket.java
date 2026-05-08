package com.qianniuyun.ticket.entity;

import com.qianniuyun.common.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String ticketNo;
    private String title;
    private String description;
    private String priority;
    private String category;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    private Long customerId;
    private String callId;
    private Long createdBy;
    private Long assignedTo;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
