package com.scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Directed edge in the workflow DAG: `job` cannot start a new run until
 * `dependsOn` has succeeded in the current scheduling cycle.
 */
@Entity
@Table(name = "job_dependencies")
@Getter
@Setter
@NoArgsConstructor
public class JobDependency {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depends_on_job_id", nullable = false)
    private Job dependsOn;
}
