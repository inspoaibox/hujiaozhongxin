package com.qianniuyun.report.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class ReportQuery {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Long> agentIds;
}
