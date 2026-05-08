package com.qianniuyun.quality.dto;

import lombok.Data;
import java.util.Map;

@Data
public class InspectionScoreDTO {
    private String callId;
    private Long templateId;
    private Map<String, Integer> scores;
    private String notes;
    private String suggestions;
}
