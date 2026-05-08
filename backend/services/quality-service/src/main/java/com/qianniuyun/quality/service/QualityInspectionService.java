package com.qianniuyun.quality.service;

import com.qianniuyun.common.exception.BusinessException;
import com.qianniuyun.quality.dto.InspectionScoreDTO;
import com.qianniuyun.quality.entity.QualityInspection;
import com.qianniuyun.quality.entity.QualityTemplate;
import com.qianniuyun.quality.repository.QualityInspectionRepository;
import com.qianniuyun.quality.repository.QualityTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通话质检服务
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityInspectionService {

    private final QualityInspectionRepository inspectionRepository;
    private final QualityTemplateRepository templateRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int PASS_SCORE_THRESHOLD = 60;

    /**
     * 提交质检评分
     */
    @Transactional
    public QualityInspection submitInspection(InspectionScoreDTO dto, Long inspectorId) {
        QualityTemplate template = templateRepository.findById(dto.getTemplateId())
                .orElseThrow(() -> new BusinessException("质检模板不存在"));

        // 计算总分
        int totalScore = calculateTotalScore(dto.getScores());

        QualityInspection inspection = new QualityInspection();
        inspection.setCallId(dto.getCallId());
        inspection.setTemplateId(dto.getTemplateId());
        inspection.setInspectorId(inspectorId);
        inspection.setScores(dto.getScores());
        inspection.setTotalScore(totalScore);
        inspection.setPassed(totalScore >= template.getPassScore());
        inspection.setNotes(dto.getNotes());
        inspection.setSuggestions(dto.getSuggestions());
        inspection.setCreatedAt(LocalDateTime.now());
        inspection.setUpdatedAt(LocalDateTime.now());

        inspectionRepository.save(inspection);

        // 不合格通话自动通知
        if (!inspection.isPassed()) {
            log.warn("质检不合格: callId={}, score={}", dto.getCallId(), totalScore);
            kafkaTemplate.send("qianniu.notification.events", "SUPERVISOR",
                    Map.of("type", "INSPECTION_FAILED",
                            "callId", dto.getCallId(),
                            "score", totalScore,
                            "passScore", template.getPassScore()));
        }

        log.info("质检完成: callId={}, score={}, passed={}", dto.getCallId(), totalScore, inspection.isPassed());
        return inspection;
    }

    /**
     * 计算质检总分
     */
    private int calculateTotalScore(Map<String, Integer> scores) {
        if (scores == null || scores.isEmpty()) return 0;
        return scores.values().stream().mapToInt(Integer::intValue).sum();
    }
}
