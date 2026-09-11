package com.scheduler.service;

import com.scheduler.entity.Job;
import com.scheduler.entity.JobDependency;
import com.scheduler.exception.CyclicDependencyException;
import com.scheduler.repository.JobDependencyRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Maintains the job-dependency DAG and answers two questions the scheduler
 * engine needs every tick:
 *   1) Is this graph acyclic? (validated whenever an edge is added)
 *   2) Given a set of jobs due to run, what order respects dependencies?
 *
 * Cycle detection uses 3-color DFS (white/gray/black) and ordering uses
 * Kahn's algorithm (BFS topological sort), both O(V + E).
 */
@Service
public class DagService {

    private final JobDependencyRepository dependencyRepository;

    public DagService(JobDependencyRepository dependencyRepository) {
        this.dependencyRepository = dependencyRepository;
    }

    /** Throws if adding edge (job -> dependsOn) would create a cycle. */
    public void assertNoCycle(UUID jobId, Set<UUID> newDependsOnIds) {
        Map<UUID, List<UUID>> adjacency = buildAdjacency();
        adjacency.computeIfAbsent(jobId, k -> new ArrayList<>()).addAll(newDependsOnIds);

        Map<UUID, Integer> color = new HashMap<>(); // 0=white,1=gray,2=black
        for (UUID node : adjacency.keySet()) {
            if (color.getOrDefault(node, 0) == 0) {
                if (hasCycleDfs(node, adjacency, color)) {
                    throw new CyclicDependencyException(
                            "Adding this dependency would create a cycle in the workflow graph");
                }
            }
        }
    }

    private boolean hasCycleDfs(UUID node, Map<UUID, List<UUID>> adjacency, Map<UUID, Integer> color) {
        color.put(node, 1); // gray = in progress
        for (UUID neighbor : adjacency.getOrDefault(node, List.of())) {
            int c = color.getOrDefault(neighbor, 0);
            if (c == 1) return true; // back-edge -> cycle
            if (c == 0 && hasCycleDfs(neighbor, adjacency, color)) return true;
        }
        color.put(node, 2); // black = done
        return false;
    }

    /**
     * Kahn's algorithm: returns the subset of `candidateJobIds` whose
     * dependencies (if also in this batch) have already been satisfied,
     * in a valid topological order. Jobs whose dependency is NOT in the
     * candidate set are assumed already satisfied (checked separately by
     * the caller against execution history).
     */
    public List<UUID> topologicalOrder(List<UUID> candidateJobIds) {
        Set<UUID> candidates = new HashSet<>(candidateJobIds);
        Map<UUID, List<UUID>> adjacency = buildAdjacency(); // node -> dependsOn list
        Map<UUID, Integer> inDegree = new HashMap<>();
        for (UUID id : candidates) inDegree.put(id, 0);

        // edge dependsOn -> node (node depends on dependsOn, so dependsOn must run first)
        Map<UUID, List<UUID>> forward = new HashMap<>();
        for (UUID node : candidates) {
            for (UUID dep : adjacency.getOrDefault(node, List.of())) {
                if (candidates.contains(dep)) {
                    forward.computeIfAbsent(dep, k -> new ArrayList<>()).add(node);
                    inDegree.merge(node, 1, Integer::sum);
                }
            }
        }

        Deque<UUID> queue = new ArrayDeque<>();
        for (var e : inDegree.entrySet()) if (e.getValue() == 0) queue.add(e.getKey());

        List<UUID> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            UUID node = queue.poll();
            ordered.add(node);
            for (UUID next : forward.getOrDefault(node, List.of())) {
                inDegree.merge(next, -1, Integer::sum);
                if (inDegree.get(next) == 0) queue.add(next);
            }
        }
        return ordered; // if a cycle slipped through, len(ordered) < candidates.size(); caller handles it
    }

    private Map<UUID, List<UUID>> buildAdjacency() {
        Map<UUID, List<UUID>> adjacency = new HashMap<>();
        for (JobDependency dep : dependencyRepository.findAll()) {
            Job job = dep.getJob();
            Job dependsOn = dep.getDependsOn();
            adjacency.computeIfAbsent(job.getId(), k -> new ArrayList<>()).add(dependsOn.getId());
        }
        return adjacency;
    }
}
