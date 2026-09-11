package com.scheduler.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CronPlannerTest {

    private final CronPlanner planner = new CronPlanner();

    @Test
    void computesNextRunForEveryFiveMinutes() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant next = planner.nextRunAfter("0/5 * * * *", from);
        assertThat(next).isAfter(from);
        assertThat(next).isBeforeOrEqualTo(from.plus(5, ChronoUnit.MINUTES));
    }

    @Test
    void validatesGoodExpression() {
        assertThat(planner.isValid("0 9 * * MON-FRI")).isTrue();
    }

    @Test
    void rejectsBadExpression() {
        assertThat(planner.isValid("not a cron")).isFalse();
    }

    @Test
    void throwsOnInvalidExpressionWhenComputingNextRun() {
        assertThatThrownBy(() -> planner.nextRunAfter("garbage", Instant.now()))
                .isInstanceOf(Exception.class);
    }
}
