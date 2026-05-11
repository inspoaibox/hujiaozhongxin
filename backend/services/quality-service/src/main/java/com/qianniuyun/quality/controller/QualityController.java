package com.qianniuyun.quality.controller;

import com.qianniuyun.common.model.PageResult;
import com.qianniuyun.common.model.Result;
import com.qianniuyun.quality.dto.InspectionScoreDTO;
import com.qianniuyun.quality.entity.QualityInspection;
import com.qianniuyun.quality.entity.QualityTemplate;
import com.qianniuyun.quality.repository.QualityInspectionRepository;
import com.qianniuyun.quality.repository.QualityTemplateRepository;
import com.qianniuyun.quality.service.QualityInspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quality")
@RequiredArgsConstructor
public class QualityController {

    private final QualityInspectionService qualityInspectionService;
    private final QualityInspectionRepository inspectionRepository;
    private final QualityTemplateRepository templateRepository;

    @GetMapping("/templates")
    public Result<PageResult<QualityTemplate>> queryTemplates(@RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "20") int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        Page<QualityTemplate> result = templateRepository.findAll(PageRequest.of(currentPage - 1, currentPageSize));
        return Result.success(PageResult.of(result.getContent(), result.getTotalElements(), currentPage, currentPageSize));
    }

    @PostMapping("/templates")
    public Result<QualityTemplate> createTemplate(@RequestBody QualityTemplate request) {
        if (request.getPassScore() == null) {
            request.setPassScore(60);
        }
        if (request.getTotalScore() == null) {
            request.setTotalScore(100);
        }
        if (request.getStatus() == null) {
            request.setStatus("ACTIVE");
        }
        return Result.success(templateRepository.save(request));
    }

    @GetMapping("/inspections")
    public Result<PageResult<QualityInspection>> queryInspections(@RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "20") int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        Page<QualityInspection> result = inspectionRepository.findAll(PageRequest.of(currentPage - 1, currentPageSize));
        return Result.success(PageResult.of(result.getContent(), result.getTotalElements(), currentPage, currentPageSize));
    }

    @PostMapping("/inspections")
    public Result<QualityInspection> submitInspection(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                                      @RequestBody InspectionScoreDTO request) {
        return Result.success(qualityInspectionService.submitInspection(request, userId != null ? userId : 0L));
    }
}
