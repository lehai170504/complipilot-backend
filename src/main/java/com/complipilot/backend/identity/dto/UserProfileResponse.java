package com.complipilot.backend.identity.dto;

import java.time.Instant;
import java.util.UUID;

import com.complipilot.backend.identity.enums.UserStatus;

public record UserProfileResponse(
        UUID id,
        String email,
        String fullName,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}