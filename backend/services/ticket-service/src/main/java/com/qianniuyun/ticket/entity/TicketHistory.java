package com.qianniuyun.ticket.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ticket_history")
public class TicketHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long ticketId;
    private String oldStatus;
    private String newStatus;
    private String comment;
    private Long operatedBy;
    private LocalDateTime createdAt;
}
