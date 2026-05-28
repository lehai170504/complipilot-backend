package com.complipilot.backend.common.security.refresh;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    long deleteByExpiresAtBefore(Instant now);

    @Modifying
    long deleteByRevokedAtIsNotNullAndRevokedAtBefore(Instant cutoff);
}