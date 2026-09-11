package com.scheduler.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketRateLimiterTest {

    @Test
    void allowsBurstUpToCapacity() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        int allowed = 0;
        for (int i = 0; i < 30; i++) {
            if (limiter.tryConsume("client-a")) allowed++;
        }
        // capacity is 20, so no more than 20 should succeed in an instant burst
        assertThat(allowed).isLessThanOrEqualTo(20);
        assertThat(allowed).isGreaterThan(0);
    }

    @Test
    void separateClientsHaveIndependentBuckets() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        for (int i = 0; i < 20; i++) {
            limiter.tryConsume("client-x"); // exhaust client-x's bucket
        }
        assertThat(limiter.tryConsume("client-x")).isFalse();
        assertThat(limiter.tryConsume("client-y")).isTrue(); // fresh bucket, unaffected
    }

    @Test
    void refillsOverTime() throws InterruptedException {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        for (int i = 0; i < 20; i++) {
            limiter.tryConsume("client-z"); // exhaust
        }
        assertThat(limiter.tryConsume("client-z")).isFalse();

        Thread.sleep(600); // refill rate is 2 tokens/sec -> should have ~1 token back

        assertThat(limiter.tryConsume("client-z")).isTrue();
    }
}
