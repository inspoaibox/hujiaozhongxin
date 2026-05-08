package com.qianniuyun.report.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AgentPerformanceService {
    public List<Map<String, Object>> aggregate(LocalDate startDate, LocalDate endDate, List<Long> agentIds) {
        // 实际实现从数据库聚合统计
        return new ArrayList<>();
    }
}
