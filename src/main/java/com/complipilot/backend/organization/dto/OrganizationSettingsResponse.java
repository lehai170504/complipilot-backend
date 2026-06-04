package com.complipilot.backend.organization.dto;

import java.time.Instant;
import java.util.UUID;

import com.complipilot.backend.organization.enums.OrganizationStatus;

public record OrganizationSettingsResponse(
        UUID id,
        String name,
        String slug,
        OrganizationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}