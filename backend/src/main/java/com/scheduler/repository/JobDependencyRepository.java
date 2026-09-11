package com.scheduler.repository;

import com.scheduler.entity.JobDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobDependencyRepository extends JpaRepository<JobDependency, UUID> {
    List<JobDependency> findByJobId(UUID jobId);
    List<JobDependency> findAllByJobIdIn(List<UUID> jobIds);
    List<JobDependency> findAll();
}
