package com.qianniuyun.ivr.controller;

import com.qianniuyun.common.model.Result;
import com.qianniuyun.ivr.engine.IVRExecutionEngine;
import com.qianniuyun.ivr.model.IVRFlow;
import com.qianniuyun.ivr.service.IVRConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ivr")
@RequiredArgsConstructor
public class IvrController {

    private final IVRExecutionEngine ivrExecutionEngine;
    private final IVRConfigService ivrConfigService;

    @GetMapping("/flows/{code}")
    public Result<IVRFlow> getFlow(@PathVariable String code) {
        return Result.success(ivrConfigService.getFlowByCode(code));
    }

    @PostMapping("/start")
    public Result<Void> start(@RequestBody Map<String, String> request) {
        ivrExecutionEngine.startIVR(request.get("callId"), request.getOrDefault("ivrCode", "DEFAULT"));
        return Result.success();
    }

    @PostMapping("/dtmf")
    public Result<Void> handleDigit(@RequestBody Map<String, String> request) {
        ivrExecutionEngine.handleDigitInput(request.get("callId"), request.get("digit"));
        return Result.success();
    }

    @PostMapping("/timeout")
    public Result<Void> handleTimeout(@RequestBody Map<String, String> request) {
        ivrExecutionEngine.handleTimeout(request.get("callId"));
        return Result.success();
    }
}
