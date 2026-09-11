package com.scheduler.controller;

import com.scheduler.dto.CreateJobRequest;
import com.scheduler.dto.JobResponse;
import com.scheduler.entity.JobStatus;
import com.scheduler.exception.RateLimitExceededException;
import com.scheduler.service.JobService;
import com.scheduler.service.TokenBucketRateLimiter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final TokenBucketRateLimiter rateLimiter;

    public JobController(JobService jobService, TokenBucketRateLimiter rateLimiter) {
        this.jobService = jobService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public List<JobResponse> listJobs() {
        return jobService.listJobs();
    }

    @GetMapping("/{id}")
    public JobResponse getJob(@PathVariable UUID id) {
        return jobService.getJob(id);
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request,
                                                  Authentication auth) {
        String caller = auth != null ? auth.getName() : "anonymous";
        if (!rateLimiter.tryConsume(caller)) {
            throw new RateLimitExceededException("Rate limit exceeded, slow down job submissions");
        }
        JobResponse created = jobService.createJob(request, caller);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/{id}/trigger")
    public ResponseEntity<Map<String, String>> triggerNow(@PathVariable UUID id, Authentication auth) {
        String caller = auth != null ? auth.getName() : "anonymous";
        if (!rateLimiter.tryConsume(caller)) {
            throw new RateLimitExceededException("Rate limit exceeded, slow down job triggers");
        }
        jobService.triggerNow(id);
        return ResponseEntity.ok(Map.of("status", "queued"));
    }

    @PatchMapping("/{id}/status")
    public JobResponse setStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        JobStatus status = JobStatus.valueOf(body.get("status").toUpperCase());
        return jobService.setStatus(id, status);
    }
}
