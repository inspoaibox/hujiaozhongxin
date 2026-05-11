package com.qianniuyun.report.controller;

import com.qianniuyun.common.model.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
public class ReportController {

    @GetMapping("/realtime-dashboard")
    public Result<Map<String, Object>> realtimeDashboard() {
        Map<String, Object> metrics = Map.of(
                "onlineAgents", 18,
                "talkingAgents", 7,
                "idleAgents", 8,
                "wrapupAgents", 2,
                "restAgents", 1,
                "queueSize", 4
        );

        Map<String, Object> todayStats = Map.of(
                "inboundCalls", 286,
                "outboundCalls", 94,
                "answerRate", 93.6,
                "avgWaitTime", 18
        );

        List<Map<String, Object>> agents = List.of(
                Map.of("agentNo", "A001", "realName", "张三", "skillGroup", "通用客服", "status", "IDLE", "statusDuration", 128, "todayCalls", 42, "avgHandleTime", 192),
                Map.of("agentNo", "A002", "realName", "李四", "skillGroup", "技术支持", "status", "TALKING", "statusDuration", 315, "todayCalls", 37, "avgHandleTime", 246),
                Map.of("agentNo", "A003", "realName", "王五", "skillGroup", "投诉处理", "status", "WRAPUP", "statusDuration", 86, "todayCalls", 29, "avgHandleTime", 221)
        );

        return Result.success(Map.of(
                "realtimeMetrics", metrics,
                "todayStats", todayStats,
                "agentStatusList", agents,
                "trendHours", List.of("09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00"),
                "inboundTrend", List.of(32, 48, 51, 30, 39, 45, 53, 41),
                "outboundTrend", List.of(12, 18, 16, 10, 14, 20, 17, 13)
        ));
    }
}
