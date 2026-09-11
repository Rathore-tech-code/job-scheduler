package com.scheduler.repository;

import com.scheduler.entity.Job;
import com.scheduler.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByStatus(JobStatus status);

    /**
     * Pulls jobs that are due to run, using Postgres row-level locking
     * (SELECT ... FOR UPDATE SKIP LOCKED) so that multiple worker instances
     * can poll concurrently without two workers claiming the same job.
     *
     * No @Lock(LockModeType...) here deliberately: Spring Data JPA cannot
     * apply a JPA lock mode to a native query -- Hibernate has no
     * setLockMode() support for NativeQuery, so combining @Lock with
     * nativeQuery = true throws "IllegalStateException: Illegal attempt to
     * set lock mode for a native query" as soon as this method is called.
     * The locking behavior isn't lost by removing it: FOR UPDATE SKIP
     * LOCKED is already expressed directly in the SQL below, which is the
     * only place a native query's row locking can be expressed.
     */
    @Query(value = "SELECT * FROM jobs " +
            "WHERE status = 'ACTIVE' AND next_run_at <= :now " +
            "ORDER BY next_run_at ASC " +
            "LIMIT :limit " +
            "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<Job> claimDueJobs(@Param("now") Instant now, @Param("limit") int limit);

    Optional<Job> findByNameIgnoreCase(String name);
}
