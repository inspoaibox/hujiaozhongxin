package com.qianniuyun.call.controller;

import com.qianniuyun.call.dto.ConferenceCallRequest;
import com.qianniuyun.call.dto.OutboundCallRequest;
import com.qianniuyun.call.dto.SummaryRequest;
import com.qianniuyun.call.dto.TransferCallRequest;
import com.qianniuyun.call.entity.Call;
import com.qianniuyun.call.service.CallService;
import com.qianniuyun.common.model.PageResult;
import com.qianniuyun.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;

    @PostMapping("/outbound")
    public Result<Call> createOutboundCall(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                           @RequestBody OutboundCallRequest request) {
        Long agentId = request.getAgentId() != null ? request.getAgentId() : userId;
        if (agentId == null) {
            agentId = 0L;
        }
        return Result.success(callService.createOutboundCall(agentId, request.getPhone()));
    }

    @GetMapping
    public Result<PageResult<Call>> queryCalls(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(callService.queryCalls(page, pageSize));
    }

    @PostMapping("/{callId}/answer")
    public Result<Void> answerCall(@PathVariable String callId) {
        callService.handleCallAnswered(callId);
        return Result.success();
    }

    @PostMapping("/{callId}/hangup")
    public Result<Void> hangupCall(@PathVariable String callId,
                                   @RequestParam(defaultValue = "NORMAL_CLEARING") String cause) {
        callService.handleCallHangup(callId, cause);
        return Result.success();
    }

    @PostMapping("/{callId}/hold")
    public Result<Void> holdCall(@PathVariable String callId) {
        callService.holdCall(callId);
        return Result.success();
    }

    @PostMapping("/{callId}/unhold")
    public Result<Void> unholdCall(@PathVariable String callId) {
        callService.unholdCall(callId);
        return Result.success();
    }

    @PostMapping("/{callId}/transfer")
    public Result<Void> transferCall(@PathVariable String callId,
                                     @RequestBody TransferCallRequest request) {
        callService.transferCall(callId, request.getTargetAgentId());
        return Result.success();
    }

    @PostMapping("/{callId}/conference")
    public Result<Void> conferenceCall(@PathVariable String callId,
                                       @RequestBody ConferenceCallRequest request) {
        callService.conferenceCall(callId, request.getThirdPartyNumber());
        return Result.success();
    }

    @PutMapping("/{callId}/summary")
    public Result<Void> updateSummary(@PathVariable String callId,
                                      @RequestBody SummaryRequest request) {
        callService.updateSummary(callId, request.getSummary());
        return Result.success();
    }

    @PostMapping("/{callId}/satisfaction")
    public Result<Void> recordSatisfaction(@PathVariable String callId,
                                           @RequestBody Map<String, Integer> request) {
        callService.recordSatisfaction(callId, request.getOrDefault("score", 5));
        return Result.success();
    }

    @GetMapping("/{callId}/recording")
    public Result<Map<String, Object>> getRecordingInfo(@PathVariable String callId) {
        return Result.success(callService.getRecordingInfo(callId));
    }
}
