package com.qianniuyun.customer.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "blacklist")
public class Blacklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String phone;
    private String reason;
    private Long createdBy;
    private Long removedBy;
    private LocalDateTime removedAt;
    private LocalDateTime createdAt;
}
