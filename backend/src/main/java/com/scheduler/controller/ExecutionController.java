package com.scheduler.controller;

import com.scheduler.dto.ExecutionResponse;
import com.scheduler.service.ExecutionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @GetMapping
    public List<ExecutionResponse> recent() {
        return executionService.recent();
    }

    @GetMapping("/job/{jobId}")
    public List<ExecutionResponse> forJob(@PathVariable UUID jobId) {
        return executionService.forJob(jobId);
    }
}
