package com.scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
public class Job {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Standard 5-field cron expression, e.g. every 5 minutes = 0/5 * * * * */
    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    /** Free-form payload handed to the job executor (JSON string). */
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.ACTIVE;

    /** Max retry attempts on failure before giving up. */
    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
