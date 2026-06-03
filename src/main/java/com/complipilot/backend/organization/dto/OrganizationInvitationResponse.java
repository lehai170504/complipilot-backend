package com.complipilot.backend.organization.dto;

import java.time.Instant;
import java.util.UUID;

import com.complipilot.backend.organization.enums.OrganizationInvitationStatus;
import com.complipilot.backend.organization.enums.OrganizationMemberRole;

public record OrganizationInvitationResponse(
        UUID id,
        UUID organizationId,
        String organizationName,
        String email,
        OrganizationMemberRole role,
        OrganizationInvitationStatus status,
        UUID invitedByUserId,
        String invitedByEmail,
        UUID acceptedByUserId,
        String acceptedByEmail,
        Instant expiresAt,
        Instant acceptedAt,
        Instant createdAt,
        Instant updatedAt,
        String invitationToken,
        String acceptUrl
) {
}
