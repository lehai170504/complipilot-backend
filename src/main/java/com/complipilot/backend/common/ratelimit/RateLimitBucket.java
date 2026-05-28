package com.complipilot.backend.common.ratelimit;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitBucket {

    private final AtomicInteger remainingTokens;
    private final Instant resetAt;

    public RateLimitBucket(
            int capacity,
            Instant resetAt
    ) {
        this.remainingTokens = new AtomicInteger(capacity);
        this.resetAt = resetAt;
    }

    public boolean tryConsume() {
        while (true) {
            int current = remainingTokens.get();

            if (current <= 0) {
                return false;
            }

            if (remainingTokens.compareAndSet(current, current - 1)) {
                return true;
            }
        }
    }

    public int remainingTokens() {
        return Math.max(remainingTokens.get(), 0);
    }

    public Instant resetAt() {
        return resetAt;
    }
}