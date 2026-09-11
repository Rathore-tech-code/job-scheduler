package com.scheduler.dto;

import com.scheduler.entity.Job;
import com.scheduler.entity.JobStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String name,
        String description,
        String cronExpression,
        JobStatus status,
        int maxRetries,
        Instant nextRunAt,
        Instant createdAt,
        List<String> dependsOn
) {
    public static JobResponse from(Job job, List<String> dependsOn) {
        return new JobResponse(
                job.getId(), job.getName(), job.getDescription(), job.getCronExpression(),
                job.getStatus(), job.getMaxRetries(), job.getNextRunAt(), job.getCreatedAt(), dependsOn
        );
    }
}
