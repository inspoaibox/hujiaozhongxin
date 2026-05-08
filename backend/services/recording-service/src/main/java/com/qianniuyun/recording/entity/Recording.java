package com.qianniuyun.recording.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "recordings")
public class Recording {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String callId;
    private String objectName;
    private Long fileSize;
    private boolean encrypted;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
