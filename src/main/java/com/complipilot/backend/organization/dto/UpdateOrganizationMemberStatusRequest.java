package com.complipilot.backend.organization.dto;

import com.complipilot.backend.organization.enums.OrganizationMemberStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationMemberStatusRequest(
        @NotNull
        OrganizationMemberStatus status
) {
}
