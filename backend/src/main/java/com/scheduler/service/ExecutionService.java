package com.scheduler.service;

import com.scheduler.dto.ExecutionResponse;
import com.scheduler.repository.ExecutionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ExecutionService {

    private final ExecutionRepository executionRepository;

    public ExecutionService(ExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    public List<ExecutionResponse> forJob(UUID jobId) {
        return executionRepository.findByJobIdOrderByQueuedAtDesc(jobId).stream()
                .map(ExecutionResponse::from)
                .toList();
    }

    public List<ExecutionResponse> recent() {
        return executionRepository.findTop50ByOrderByQueuedAtDesc().stream()
                .map(ExecutionResponse::from)
                .toList();
    }
}
