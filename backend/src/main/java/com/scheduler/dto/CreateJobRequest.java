package com.scheduler.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateJobRequest(
        @NotBlank String name,
        String description,
        @NotBlank String cronExpression,
        String payload,
        Integer maxRetries,
        List<String> dependsOnJobNames
) {}
