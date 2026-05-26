package com.complipilot.backend.identity.dto.register;

import java.util.UUID;

import com.complipilot.backend.organization.enums.OrganizationMemberRole;

public record RegisterResponse(
        UUID userId,
        UUID organizationId,
        String email,
        String fullName,
        String organizationName,
        OrganizationMemberRole role
) {
}