package com.qianniuyun.report.service;

import com.qianniuyun.common.exception.BusinessException;
import com.qianniuyun.report.dto.ReportQuery;
import com.qianniuyun.report.entity.Report;
import com.qianniuyun.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 报表服务
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final CallStatisticsService callStatisticsService;
    private final AgentPerformanceService agentPerformanceService;

    /**
     * 生成座席绩效报表
     */
    @Async
    public void generateAgentPerformanceReport(ReportQuery query, Long requestedBy) {
        Report report = createReportRecord("AGENT_PERFORMANCE", query, requestedBy);

        try {
            List<Map<String, Object>> data = agentPerformanceService.aggregate(
                    query.getStartDate(), query.getEndDate(), query.getAgentIds());

            report.setData(data);
            report.setStatus("COMPLETED");
            report.setCompletedAt(LocalDateTime.now());
            reportRepository.save(report);

            log.info("座席绩效报表生成完成: reportId={}", report.getId());
        } catch (Exception e) {
            report.setStatus("FAILED");
            reportRepository.save(report);
            log.error("座席绩效报表生成失败: reportId={}", report.getId(), e);
        }
    }

    /**
     * 生成呼叫统计报表
     */
    @Async
    public void generateCallStatisticsReport(ReportQuery query, Long requestedBy) {
        Report report = createReportRecord("CALL_STATISTICS", query, requestedBy);

        try {
            Map<String, Object> stats = callStatisticsService.aggregate(
                    query.getStartDate(), query.getEndDate());

            report.setData(List.of(stats));
            report.setStatus("COMPLETED");
            report.setCompletedAt(LocalDateTime.now());
            reportRepository.save(report);

            log.info("呼叫统计报表生成完成: reportId={}", report.getId());
        } catch (Exception e) {
            report.setStatus("FAILED");
            reportRepository.save(report);
            log.error("呼叫统计报表生成失败", e);
        }
    }

    /**
     * 导出报表为 Excel
     */
    public byte[] exportToExcel(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("报表不存在"));

        if (!"COMPLETED".equals(report.getStatus())) {
            throw new BusinessException("报表尚未生成完成");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("报表数据");

            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 写入表头
            List<String> headers = getHeaders(report.getReportType());
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // 写入数据
            List<Map<String, Object>> data = report.getData();
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                fillRowData(row, data.get(i), headers);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new BusinessException("导出Excel失败: " + e.getMessage());
        }
    }

    /**
     * 定时生成日报（每天8点）
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void generateDailyReport() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        ReportQuery query = new ReportQuery();
        query.setStartDate(yesterday);
        query.setEndDate(yesterday);

        generateCallStatisticsReport(query, 1L); // 系统自动生成
        generateAgentPerformanceReport(query, 1L);

        log.info("日报生成任务已触发: date={}", yesterday);
    }

    private Report createReportRecord(String type, ReportQuery query, Long requestedBy) {
        Report report = new Report();
        report.setReportType(type);
        report.setName(type + "_" + query.getStartDate() + "_" + query.getEndDate());
        report.setStartDate(query.getStartDate());
        report.setEndDate(query.getEndDate());
        report.setStatus("PENDING");
        report.setCreatedBy(requestedBy);
        report.setCreatedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    private List<String> getHeaders(String reportType) {
        return switch (reportType) {
            case "AGENT_PERFORMANCE" -> List.of("座席工号", "姓名", "接听量", "总通话时长(分)", "平均处理时长(秒)", "满意度");
            case "CALL_STATISTICS" -> List.of("日期", "呼入量", "呼出量", "接通率", "放弃率", "平均等待时长(秒)");
            case "SATISFACTION" -> List.of("座席工号", "姓名", "评价次数", "平均分", "5分占比", "1分占比");
            default -> List.of("数据");
        };
    }

    private void fillRowData(Row row, Map<String, Object> data, List<String> headers) {
        int col = 0;
        for (String header : headers) {
            Cell cell = row.createCell(col++);
            Object value = data.get(header);
            if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            } else if (value != null) {
                cell.setCellValue(value.toString());
            }
        }
    }
}
