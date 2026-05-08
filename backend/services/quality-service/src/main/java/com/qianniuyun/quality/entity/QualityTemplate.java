package com.qianniuyun.quality.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "quality_templates")
public class QualityTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Integer totalScore;
    private Integer passScore;
    private String status;
}
