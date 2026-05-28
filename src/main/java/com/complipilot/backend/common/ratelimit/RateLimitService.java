package com.complipilot.backend.common.ratelimit;

import java.time.Instant;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final RateLimitProperties rateLimitProperties;
    private final Cache<String, RateLimitBucket> authBuckets;

    public RateLimitService(RateLimitProperties rateLimitProperties) {
        this.rateLimitProperties = rateLimitProperties;
        this.authBuckets = Caffeine.newBuilder()
                .expireAfterWrite(
                        rateLimitProperties.authWindowSeconds(),
                        java.util.concurrent.TimeUnit.SECONDS
                )
                .maximumSize(100_000)
                .build();
    }

    public RateLimitDecision consumeAuthToken(String key) {
        if (!rateLimitProperties.enabled()) {
            return RateLimitDecision.allowed(
                    rateLimitProperties.authCapacity(),
                    Instant.now().plusSeconds(rateLimitProperties.authWindowSeconds())
            );
        }

        Instant resetAt = Instant.now().plusSeconds(rateLimitProperties.authWindowSeconds());

        RateLimitBucket bucket = authBuckets.get(
                key,
                ignored -> new RateLimitBucket(
                        rateLimitProperties.authCapacity(),
                        resetAt
                )
        );

        boolean allowed = bucket.tryConsume();

        if (allowed) {
            return RateLimitDecision.allowed(
                    bucket.remainingTokens(),
                    bucket.resetAt()
            );
        }

        return RateLimitDecision.rejected(
                bucket.remainingTokens(),
                bucket.resetAt()
        );
    }
}