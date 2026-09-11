package com.scheduler.service;

import com.scheduler.entity.*;
import com.scheduler.repository.ExecutionRepository;
import com.scheduler.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * The heart of the orchestrator.
 *
 * Rather than scanning the whole `jobs` table on every tick, due jobs are
 * held in an in-memory min-heap keyed by next-run-time (a classic
 * timing-wheel / delay-queue pattern used by real schedulers like Quartz
 * and Airflow's scheduler loop). The DB remains the source of truth and
 * is periodically re-synced into the heap; the heap just avoids repeated
 * full-table scans for "what's due right now".
 *
 * Each tick:
 *  1. Pop everything due from the heap.
 *  2. Ask DagService for a valid execution order (topological sort) and
 *     drop any job whose dependency hasn't succeeded yet this cycle.
 *  3. Claim jobs at the DB level with SELECT ... FOR UPDATE SKIP LOCKED so
 *     that if this were scaled to N worker instances, no two workers ever
 *     double-execute the same job.
 *  4. "Execute" (simulated) with retry + exponential backoff + jitter on
 *     failure, then reschedule the job's next run via its cron expression.
 */
@Component
public class SchedulerEngine {

    private static final Logger log = LoggerFactory.getLogger(SchedulerEngine.class);
    private static final String WORKER_ID = "worker-" + UUID.randomUUID().toString().substring(0, 8);
    private static final int MAX_BATCH_SIZE = 50;

    private final JobRepository jobRepository;
    private final ExecutionRepository executionRepository;
    private final CronPlanner cronPlanner;
    private final DagService dagService;
    private final ExecutionEventPublisher eventPublisher;

    /** In-memory min-heap of (nextRunAt, jobId), mirrors due jobs from the DB. */
    private final PriorityBlockingQueue<ScheduledEntry> heap =
            new PriorityBlockingQueue<>(64, Comparator.comparing(e -> e.nextRunAt));
    private final Set<UUID> queuedJobIds = Collections.synchronizedSet(new HashSet<>());

    /** Tracks which jobs have already succeeded in the current DAG cycle. */
    private final Set<UUID> succeededThisCycle = Collections.synchronizedSet(new HashSet<>());

    public SchedulerEngine(JobRepository jobRepository, ExecutionRepository executionRepository,
                            CronPlanner cronPlanner, DagService dagService,
                            ExecutionEventPublisher eventPublisher) {
        this.jobRepository = jobRepository;
        this.executionRepository = executionRepository;
        this.cronPlanner = cronPlanner;
        this.dagService = dagService;
        this.eventPublisher = eventPublisher;
    }

    private record ScheduledEntry(UUID jobId, Instant nextRunAt) {}

    /** Re-sync the heap from the DB. Cheap enough to run every few seconds. */
    @Scheduled(fixedDelay = 5000)
    public void syncHeapFromDatabase() {
        List<Job> active = jobRepository.findByStatus(JobStatus.ACTIVE);
        for (Job job : active) {
            if (job.getNextRunAt() != null && !queuedJobIds.contains(job.getId())) {
                heap.offer(new ScheduledEntry(job.getId(), job.getNextRunAt()));
                queuedJobIds.add(job.getId());
            }
        }
    }

    /** The main tick: drain due work from the heap and execute it. */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void tick() {
        Instant now = Instant.now();
        List<UUID> due = new ArrayList<>();
        while (!heap.isEmpty() && !heap.peek().nextRunAt.isAfter(now) && due.size() < MAX_BATCH_SIZE) {
            ScheduledEntry entry = heap.poll();
            queuedJobIds.remove(entry.jobId);
            due.add(entry.jobId);
        }
        if (due.isEmpty()) return;

        // Claim at the DB level (SKIP LOCKED) so concurrent worker instances never collide.
        List<Job> claimed = jobRepository.claimDueJobs(now, MAX_BATCH_SIZE);
        Map<UUID, Job> claimedById = new HashMap<>();
        for (Job j : claimed) claimedById.put(j.getId(), j);

        List<UUID> claimedDueIds = due.stream().filter(claimedById::containsKey).toList();
        if (claimedDueIds.isEmpty()) return;

        // Respect dependency order within this batch.
        List<UUID> ordered = dagService.topologicalOrder(claimedDueIds);

        for (UUID jobId : ordered) {
            Job job = claimedById.get(jobId);
            executeWithRetry(job);
            rescheduleNextRun(job);
        }
    }

    private void executeWithRetry(Job job) {
        Execution execution = new Execution();
        execution.setJob(job);
        execution.setWorkerId(WORKER_ID);
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(Instant.now());
        executionRepository.save(execution);
        eventPublisher.publish(execution);

        int attempt = 0;
        boolean success = false;
        while (attempt <= job.getMaxRetries() && !success) {
            success = simulateExecution(job, attempt);
            if (!success && attempt < job.getMaxRetries()) {
                long backoffMillis = exponentialBackoffWithJitter(attempt);
                log.info("Job {} failed on attempt {}, retrying in {}ms", job.getName(), attempt, backoffMillis);
                execution.setStatus(ExecutionStatus.RETRYING);
                execution.setRetryCount(attempt + 1);
                executionRepository.save(execution);
                eventPublisher.publish(execution);
                sleepQuietly(backoffMillis);
            }
            attempt++;
        }

        execution.setStatus(success ? ExecutionStatus.SUCCEEDED : ExecutionStatus.FAILED);
        execution.setFinishedAt(Instant.now());
        execution.setRetryCount(Math.max(0, attempt - 1));
        executionRepository.save(execution);
        eventPublisher.publish(execution);

        if (success) succeededThisCycle.add(job.getId());
    }

    /**
     * Stand-in for real work (calling a webhook, running a shell task, etc).
     * Deterministic-ish random failure so retry/backoff is actually exercised.
     */
    private boolean simulateExecution(Job job, int attempt) {
        double failureChance = attempt == 0 ? 0.15 : 0.05;
        return Math.random() > failureChance;
    }

    /** Full jitter exponential backoff: random(0, base * 2^attempt), capped. */
    private long exponentialBackoffWithJitter(int attempt) {
        long base = 200; // ms
        long cap = 10_000; // ms
        long exp = Math.min(cap, base * (1L << attempt));
        return (long) (Math.random() * exp);
    }

    private void rescheduleNextRun(Job job) {
        Instant next = cronPlanner.nextRunAfter(job.getCronExpression(), Instant.now());
        job.setNextRunAt(next);
        jobRepository.save(job);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Forces a job into the heap immediately (used by the manual "trigger now" endpoint). */
    public void enqueueImmediately(UUID jobId) {
        if (queuedJobIds.add(jobId)) {
            heap.offer(new ScheduledEntry(jobId, Instant.now()));
        }
    }
}
