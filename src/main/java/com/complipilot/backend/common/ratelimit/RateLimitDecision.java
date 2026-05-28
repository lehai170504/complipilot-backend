package com.complipilot.backend.common.ratelimit;

import java.time.Instant;

public record RateLimitDecision(
        boolean allowed,
        int remaining,
        Instant resetAt
) {

    public static RateLimitDecision allowed(
            int remaining,
            Instant resetAt
    ) {
        return new RateLimitDecision(true, remaining, resetAt);
    }

    public static RateLimitDecision rejected(
            int remaining,
            Instant resetAt
    ) {
        return new RateLimitDecision(false, remaining, resetAt);
    }
}