package com.complipilot.backend.organization.dto;

import com.complipilot.backend.organization.enums.OrganizationMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrganizationInvitationRequest(
        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotNull
        OrganizationMemberRole role
) {
}
