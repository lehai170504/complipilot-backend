package com.complipilot.backend.organization.dto;

import java.util.UUID;

import com.complipilot.backend.organization.enums.OrganizationMemberRole;
import com.complipilot.backend.organization.enums.OrganizationMemberStatus;

public record OrganizationMembershipResponse(
        UUID organizationId,
        String organizationName,
        String organizationSlug,
        OrganizationMemberRole role,
        OrganizationMemberStatus status
) {
}