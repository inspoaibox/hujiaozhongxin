package com.qianniuyun.quality.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Entity
@Table(name = "quality_inspections")
public class QualityInspection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String callId;
    private Long templateId;
    private Long inspectorId;

    @ElementCollection
    @CollectionTable(name = "inspection_scores", joinColumns = @JoinColumn(name = "inspection_id"))
    @MapKeyColumn(name = "item_key")
    @Column(name = "score")
    private Map<String, Integer> scores;

    private Integer totalScore;
    private boolean passed;
    private String notes;
    private String suggestions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
