package com.scheduler.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-client token bucket rate limiter, used to cap how many jobs a given
 * API caller can submit or trigger per minute. Lock-free (CAS-based),
 * O(1) per check, and self-refilling (no background thread needed).
 */
@Component
public class TokenBucketRateLimiter {

    private static final double REFILL_TOKENS_PER_SECOND = 2.0; // steady-state throughput
    private static final long BUCKET_CAPACITY = 20;             // allowed burst

    private static final class Bucket {
        final AtomicLong tokensMilli = new AtomicLong(BUCKET_CAPACITY * 1000);
        volatile long lastRefillNanos = System.nanoTime();
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** Returns true if the caller is allowed to proceed (a token was consumed). */
    public boolean tryConsume(String clientKey) {
        Bucket bucket = buckets.computeIfAbsent(clientKey, k -> new Bucket());
        refill(bucket);

        long current;
        do {
            current = bucket.tokensMilli.get();
            if (current < 1000) {
                return false; // less than one whole token available
            }
        } while (!bucket.tokensMilli.compareAndSet(current, current - 1000));
        return true;
    }

    private void refill(Bucket bucket) {
        long now = System.nanoTime();
        long elapsedNanos = now - bucket.lastRefillNanos;
        if (elapsedNanos <= 0) return;

        long addedMilliTokens = (long) (elapsedNanos / 1_000_000_000.0 * REFILL_TOKENS_PER_SECOND * 1000);
        if (addedMilliTokens <= 0) return;

        bucket.lastRefillNanos = now;
        long cap = BUCKET_CAPACITY * 1000;
        bucket.tokensMilli.updateAndGet(t -> Math.min(cap, t + addedMilliTokens));
    }
}
