package com.complipilot.backend.common.security.refresh;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final long revokedRefreshTokenRetentionSeconds;

    public RefreshTokenCleanupJob(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.revoked-refresh-token-retention-seconds}") long revokedRefreshTokenRetentionSeconds
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.revokedRefreshTokenRetentionSeconds = revokedRefreshTokenRetentionSeconds;
    }

    @Scheduled(fixedRateString = "${app.jwt.refresh-token-cleanup-fixed-rate-ms}")
    @Transactional
    public void cleanupRefreshTokens() {
        Instant now = Instant.now();
        Instant revokedCutoff = now.minusSeconds(revokedRefreshTokenRetentionSeconds);

        long expiredDeleted = refreshTokenRepository.deleteByExpiresAtBefore(now);
        long revokedDeleted = refreshTokenRepository.deleteByRevokedAtIsNotNullAndRevokedAtBefore(revokedCutoff);

        if (expiredDeleted > 0 || revokedDeleted > 0) {
            log.info(
                    "Cleaned up refresh tokens: expiredDeleted={}, revokedDeleted={}",
                    expiredDeleted,
                    revokedDeleted
            );
        }
    }
}