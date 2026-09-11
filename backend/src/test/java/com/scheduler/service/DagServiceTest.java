package com.scheduler.service;

import com.scheduler.entity.Job;
import com.scheduler.entity.JobDependency;
import com.scheduler.exception.CyclicDependencyException;
import com.scheduler.repository.JobDependencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DagServiceTest {

    @Mock
    private JobDependencyRepository dependencyRepository;

    private DagService dagService;

    private UUID a, b, c, d;

    @BeforeEach
    void setUp() {
        dagService = new DagService(dependencyRepository);
        a = UUID.randomUUID();
        b = UUID.randomUUID();
        c = UUID.randomUUID();
        d = UUID.randomUUID();
    }

    private Job jobWithId(UUID id) {
        Job job = new Job();
        job.setId(id);
        return job;
    }

    private JobDependency edge(UUID jobId, UUID dependsOnId) {
        JobDependency dep = new JobDependency();
        dep.setJob(jobWithId(jobId));
        dep.setDependsOn(jobWithId(dependsOnId));
        return dep;
    }

    @Test
    void detectsNoCycleOnEmptyGraph() {
        when(dependencyRepository.findAll()).thenReturn(List.of());
        // a depends on b: fine, no existing edges
        dagService.assertNoCycle(a, Set.of(b));
        // no exception = pass
    }

    @Test
    void detectsDirectCycle() {
        // existing edge: b depends on a
        when(dependencyRepository.findAll()).thenReturn(List.of(edge(b, a)));
        // adding: a depends on b => cycle a->b->a
        assertThatThrownBy(() -> dagService.assertNoCycle(a, Set.of(b)))
                .isInstanceOf(CyclicDependencyException.class);
    }

    @Test
    void detectsTransitiveCycle() {
        // existing: b depends on a, c depends on b
        when(dependencyRepository.findAll()).thenReturn(List.of(edge(b, a), edge(c, b)));
        // adding: a depends on c => cycle a->c->b->a
        assertThatThrownBy(() -> dagService.assertNoCycle(a, Set.of(c)))
                .isInstanceOf(CyclicDependencyException.class);
    }

    @Test
    void allowsDiamondDependencyWithoutCycle() {
        // d depends on b and c; b and c both depend on a. No cycle.
        when(dependencyRepository.findAll()).thenReturn(List.of(
                edge(b, a), edge(c, a), edge(d, b)
        ));
        dagService.assertNoCycle(d, Set.of(c)); // should not throw
    }

    @Test
    void topologicalOrderRespectsDependencies() {
        // c depends on b, b depends on a => valid order must have a before b before c
        when(dependencyRepository.findAll()).thenReturn(List.of(edge(b, a), edge(c, b)));

        List<UUID> order = dagService.topologicalOrder(List.of(c, a, b));

        assertThat(order).containsExactly(a, b, c);
    }

    @Test
    void topologicalOrderHandlesIndependentJobs() {
        when(dependencyRepository.findAll()).thenReturn(List.of());
        List<UUID> order = dagService.topologicalOrder(List.of(a, b, c));
        assertThat(order).containsExactlyInAnyOrder(a, b, c);
    }
}
