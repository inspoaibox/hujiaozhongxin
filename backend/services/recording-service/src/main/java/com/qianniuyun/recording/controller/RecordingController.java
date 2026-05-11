package com.qianniuyun.recording.controller;

import com.qianniuyun.common.exception.BusinessException;
import com.qianniuyun.common.model.Result;
import com.qianniuyun.recording.entity.Recording;
import com.qianniuyun.recording.repository.RecordingRepository;
import com.qianniuyun.recording.service.RecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/recordings")
@RequiredArgsConstructor
public class RecordingController {

    private final RecordingRepository recordingRepository;
    private final RecordingService recordingService;

    @GetMapping("/{callId}")
    public Result<Recording> getRecording(@PathVariable String callId) {
        Recording recording = recordingRepository.findByCallId(callId)
                .orElseThrow(() -> new BusinessException("录音不存在: " + callId));
        return Result.success(recording);
    }

    @GetMapping("/{callId}/stream")
    public ResponseEntity<InputStreamResource> streamRecording(@PathVariable String callId) throws Exception {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .body(new InputStreamResource(recordingService.getRecordingStream(callId)));
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.success(Map.of("service", "recording-service", "status", "UP"));
    }
}
