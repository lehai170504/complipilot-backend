package com.complipilot.backend.organization.dto;

import com.complipilot.backend.organization.enums.OrganizationMemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationMemberRoleRequest(
        @NotNull
        OrganizationMemberRole role
) {
}
