package com.complipilot.backend.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationSettingsRequest(
        @NotBlank
        @Size(max = 200)
        String name
) {
}