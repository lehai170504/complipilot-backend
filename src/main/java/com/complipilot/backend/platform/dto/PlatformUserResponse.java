package com.complipilot.backend.platform.dto;

import java.time.Instant;
import java.util.UUID;

import com.complipilot.backend.identity.enums.UserStatus;

public record PlatformUserResponse(
        UUID id,
        String email,
        String fullName,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}