package com.scheduler.service;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Wraps cron-utils to turn a 5-field unix cron expression into the next
 * execution instant. Kept behind a thin interface so the scheduling engine
 * doesn't care how "next run" is computed.
 */
@Component
public class CronPlanner {

    private final CronParser parser =
            new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

    public Instant nextRunAfter(String cronExpression, Instant from) {
        Cron cron = parser.parse(cronExpression);
        cron.validate();
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        ZonedDateTime zdt = from.atZone(ZoneOffset.UTC);
        Optional<ZonedDateTime> next = executionTime.nextExecution(zdt);
        return next.map(ZonedDateTime::toInstant)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cron expression '" + cronExpression + "' has no future execution"));
    }

    public boolean isValid(String cronExpression) {
        try {
            parser.parse(cronExpression).validate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
