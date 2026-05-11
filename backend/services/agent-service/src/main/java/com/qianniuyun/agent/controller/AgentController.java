package com.qianniuyun.agent.controller;

import com.qianniuyun.agent.dto.AgentStatusRequest;
import com.qianniuyun.agent.dto.TrainingModeRequest;
import com.qianniuyun.agent.entity.Agent;
import com.qianniuyun.agent.repository.AgentRepository;
import com.qianniuyun.agent.service.AgentStatusService;
import com.qianniuyun.common.enums.AgentStatus;
import com.qianniuyun.common.exception.BusinessException;
import com.qianniuyun.common.model.PageResult;
import com.qianniuyun.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentRepository agentRepository;
    private final AgentStatusService agentStatusService;

    @GetMapping
    public Result<PageResult<Agent>> queryAgents(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        Page<Agent> result = agentRepository.findAll(PageRequest.of(currentPage - 1, currentPageSize));
        return Result.success(PageResult.of(result.getContent(), result.getTotalElements(), currentPage, currentPageSize));
    }

    @GetMapping("/{agentId}")
    public Result<Agent> getAgent(@PathVariable Long agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new BusinessException("座席不存在"));
        return Result.success(agent);
    }

    @PutMapping("/{agentId}/status")
    public Result<Void> updateStatus(@PathVariable Long agentId,
                                     @RequestBody AgentStatusRequest request) {
        if (request.getStatus() == null) {
            throw new BusinessException("座席状态不能为空");
        }
        agentStatusService.updateStatus(agentId, AgentStatus.valueOf(request.getStatus()));
        return Result.success();
    }

    @PostMapping("/{agentId}/login")
    public Result<Void> login(@PathVariable Long agentId) {
        agentStatusService.agentLogin(agentId);
        return Result.success();
    }

    @PostMapping("/{agentId}/logout")
    public Result<Void> logout(@PathVariable Long agentId) {
        agentStatusService.agentLogout(agentId);
        return Result.success();
    }

    @PutMapping("/{agentId}/training-mode")
    public Result<Agent> updateTrainingMode(@PathVariable Long agentId,
                                            @RequestBody TrainingModeRequest request) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new BusinessException("座席不存在"));
        agent.setTrainingMode(request.isEnabled());
        agent.setUpdatedAt(LocalDateTime.now());
        Agent saved = agentRepository.save(agent);
        if (saved.getStatus() == AgentStatus.IDLE) {
            agentStatusService.updateStatus(saved.getId(), AgentStatus.IDLE);
        }
        return Result.success(saved);
    }

    @GetMapping("/skill-groups")
    public Result<List<Map<String, Object>>> skillGroups() {
        return Result.success(List.of(
                Map.<String, Object>of("id", 1L, "code", "GENERAL", "name", "通用客服", "agentCount", 0),
                Map.<String, Object>of("id", 2L, "code", "TECH", "name", "技术支持", "agentCount", 0),
                Map.<String, Object>of("id", 3L, "code", "COMPLAINT", "name", "投诉处理", "agentCount", 0),
                Map.<String, Object>of("id", 4L, "code", "VIP", "name", "VIP 专席", "agentCount", 0)
        ));
    }
}
