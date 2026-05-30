package com.complipilot.backend.organization.dto;

import java.time.Instant;
import java.util.UUID;

import com.complipilot.backend.organization.enums.OrganizationMemberRole;
import com.complipilot.backend.organization.enums.OrganizationMemberStatus;

public record OrganizationMemberResponse(
        UUID id,
        UUID organizationId,
        UUID userId,
        String email,
        String fullName,
        OrganizationMemberRole role,
        OrganizationMemberStatus status,
        Instant joinedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
