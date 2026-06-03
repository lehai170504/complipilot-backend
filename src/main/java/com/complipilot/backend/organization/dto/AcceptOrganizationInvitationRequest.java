package com.complipilot.backend.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptOrganizationInvitationRequest(
        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(max = 150)
        String fullName,

        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {
}
