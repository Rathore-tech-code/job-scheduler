package com.scheduler.repository;

import org.springframework.data.jpa.repository.Query;
import com.scheduler.entity.Execution;
import com.scheduler.entity.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExecutionRepository extends JpaRepository<Execution, UUID> {
    @Query("SELECT e FROM Execution e JOIN FETCH e.job WHERE e.job.id = :jobId ORDER BY e.queuedAt DESC")
    List<Execution> findByJobIdOrderByQueuedAtDesc(UUID jobId);
    Optional<Execution> findFirstByJobIdAndStatusOrderByQueuedAtDesc(UUID jobId, ExecutionStatus status);
    @Query("SELECT e FROM Execution e JOIN FETCH e.job ORDER BY e.queuedAt DESC")
    List<Execution> findTop50ByOrderByQueuedAtDesc();
}
