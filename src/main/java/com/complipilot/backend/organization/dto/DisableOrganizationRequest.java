package com.complipilot.backend.organization.dto;

import jakarta.validation.constraints.NotBlank;

public record DisableOrganizationRequest(
        @NotBlank
        String confirmation
) {
}