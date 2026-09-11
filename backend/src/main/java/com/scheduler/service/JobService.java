package com.scheduler.service;

import com.scheduler.dto.CreateJobRequest;
import com.scheduler.dto.JobResponse;
import com.scheduler.entity.Job;
import com.scheduler.entity.JobDependency;
import com.scheduler.entity.JobStatus;
import com.scheduler.exception.JobNotFoundException;
import com.scheduler.repository.JobDependencyRepository;
import com.scheduler.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobDependencyRepository dependencyRepository;
    private final CronPlanner cronPlanner;
    private final DagService dagService;
    private final SchedulerEngine schedulerEngine;

    public JobService(JobRepository jobRepository, JobDependencyRepository dependencyRepository,
                       CronPlanner cronPlanner, DagService dagService, SchedulerEngine schedulerEngine) {
        this.jobRepository = jobRepository;
        this.dependencyRepository = dependencyRepository;
        this.cronPlanner = cronPlanner;
        this.dagService = dagService;
        this.schedulerEngine = schedulerEngine;
    }

    @Transactional
    public JobResponse createJob(CreateJobRequest request, String createdBy) {
        if (!cronPlanner.isValid(request.cronExpression())) {
            throw new IllegalArgumentException("Invalid cron expression: " + request.cronExpression());
        }

        Job job = new Job();
        job.setName(request.name());
        job.setDescription(request.description());
        job.setCronExpression(request.cronExpression());
        job.setPayload(request.payload());
        job.setMaxRetries(request.maxRetries() != null ? request.maxRetries() : 3);
        job.setCreatedBy(createdBy);
        job.setNextRunAt(cronPlanner.nextRunAfter(request.cronExpression(), Instant.now()));
        job = jobRepository.save(job);

        List<String> dependsOnNames = List.of();
        if (request.dependsOnJobNames() != null && !request.dependsOnJobNames().isEmpty()) {
            List<Job> dependsOnJobs = request.dependsOnJobNames().stream()
                    .map(n -> jobRepository.findByNameIgnoreCase(n)
                            .orElseThrow(() -> new JobNotFoundException("Dependency job not found: " + n)))
                    .toList();

            Set<UUID> dependsOnIds = dependsOnJobs.stream().map(Job::getId).collect(Collectors.toSet());
            dagService.assertNoCycle(job.getId(), dependsOnIds); // validates before persisting edges

            for (Job dep : dependsOnJobs) {
                JobDependency edge = new JobDependency();
                edge.setJob(job);
                edge.setDependsOn(dep);
                dependencyRepository.save(edge);
            }
            dependsOnNames = dependsOnJobs.stream().map(Job::getName).toList();
        }

        return JobResponse.from(job, dependsOnNames);
    }

    public JobResponse getJob(UUID id) {
        Job job = findOrThrow(id);
        return JobResponse.from(job, dependencyNames(job));
    }

    public List<JobResponse> listJobs() {
        return jobRepository.findAll().stream()
                .map(j -> JobResponse.from(j, dependencyNames(j)))
                .toList();
    }

    @Transactional
    public void triggerNow(UUID id) {
        Job job = findOrThrow(id);
        schedulerEngine.enqueueImmediately(job.getId());
    }

    @Transactional
    public JobResponse setStatus(UUID id, JobStatus status) {
        Job job = findOrThrow(id);
        job.setStatus(status);
        jobRepository.save(job);
        return JobResponse.from(job, dependencyNames(job));
    }

    private List<String> dependencyNames(Job job) {
        return dependencyRepository.findByJobId(job.getId()).stream()
                .map(d -> d.getDependsOn().getName())
                .toList();
    }

    private Job findOrThrow(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found: " + id));
    }
}
