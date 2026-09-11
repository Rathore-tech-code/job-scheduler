package com.scheduler.repository;

import com.scheduler.entity.Job;
import com.scheduler.entity.JobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises JobRepository.claimDueJobs() directly.
 *
 * This is the exact method that previously threw
 * "IllegalStateException: Illegal attempt to set lock mode for a native
 * query" because it combined @Lock(LockModeType.PESSIMISTIC_WRITE) with
 * nativeQuery = true -- a combination Spring Data JPA/Hibernate does not
 * support (locking on a native query can only be expressed in the SQL
 * itself, which is what "FOR UPDATE SKIP LOCKED" in the query already
 * does). Simply calling this method without it throwing is itself the
 * regression check for that bug; the assertions below additionally confirm
 * the query's actual filtering/ordering/limit behavior wasn't changed by
 * removing the annotation.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class JobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void claimDueJobsDoesNotThrowAndReturnsOnlyDueActiveJobs() {
        Instant now = Instant.now();

        Job dueActive = newJob("due-active", JobStatus.ACTIVE, now.minus(1, ChronoUnit.MINUTES));
        Job notYetDue = newJob("not-yet-due", JobStatus.ACTIVE, now.plus(1, ChronoUnit.HOURS));
        Job duePaused = newJob("due-paused", JobStatus.PAUSED, now.minus(1, ChronoUnit.MINUTES));

        entityManager.persistAndFlush(dueActive);
        entityManager.persistAndFlush(notYetDue);
        entityManager.persistAndFlush(duePaused);

        // The call that used to throw IllegalStateException before the fix.
        List<Job> claimed = jobRepository.claimDueJobs(now, 50);

        assertThat(claimed)
                .extracting(Job::getName)
                .containsExactly("due-active"); // only the due + ACTIVE job, nothing else
    }

    @Test
    void claimDueJobsRespectsTheLimitParameter() {
        Instant now = Instant.now();
        for (int i = 0; i < 5; i++) {
            entityManager.persistAndFlush(newJob("job-" + i, JobStatus.ACTIVE, now.minus(1, ChronoUnit.MINUTES)));
        }

        List<Job> claimed = jobRepository.claimDueJobs(now, 3);

        assertThat(claimed).hasSize(3);
    }

    @Test
    void claimDueJobsOrdersByNextRunAtAscending() {
        Instant now = Instant.now();
        Job later = newJob("later", JobStatus.ACTIVE, now.minus(1, ChronoUnit.MINUTES));
        Job earliest = newJob("earliest", JobStatus.ACTIVE, now.minus(10, ChronoUnit.MINUTES));
        Job middle = newJob("middle", JobStatus.ACTIVE, now.minus(5, ChronoUnit.MINUTES));

        entityManager.persistAndFlush(later);
        entityManager.persistAndFlush(earliest);
        entityManager.persistAndFlush(middle);

        List<Job> claimed = jobRepository.claimDueJobs(now, 50);

        assertThat(claimed).extracting(Job::getName).containsExactly("earliest", "middle", "later");
    }

    private Job newJob(String name, JobStatus status, Instant nextRunAt) {
        Job job = new Job();
        job.setName(name);
        job.setCronExpression("0 * * * *");
        job.setStatus(status);
        job.setNextRunAt(nextRunAt);
        return job;
    }
}
