package com.scheduler.dto;

import com.scheduler.entity.Execution;
import com.scheduler.entity.ExecutionStatus;

import java.time.Instant;
import java.util.UUID;

public record ExecutionResponse(
        UUID id,
        UUID jobId,
        String jobName,
        ExecutionStatus status,
        String workerId,
        int retryCount,
        Instant queuedAt,
        Instant startedAt,
        Instant finishedAt
) {
    public static ExecutionResponse from(Execution e) {
        return new ExecutionResponse(
                e.getId(), e.getJob().getId(), e.getJob().getName(), e.getStatus(),
                e.getWorkerId(), e.getRetryCount(), e.getQueuedAt(), e.getStartedAt(), e.getFinishedAt()
        );
    }
}
