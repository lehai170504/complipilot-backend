package com.complipilot.backend.compliance.dto.requirement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRequirementRequest(
        @NotBlank
        @Size(max = 100)
        String code,

        @NotBlank
        @Size(max = 250)
        String title,

        @Size(max = 10000)
        String description,

        @Size(max = 120)
        String category,

        int sortOrder
) {
}