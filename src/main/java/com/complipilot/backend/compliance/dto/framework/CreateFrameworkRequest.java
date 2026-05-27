package com.complipilot.backend.compliance.dto.framework;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFrameworkRequest(
        @NotBlank
        @Size(max = 80)
        String code,

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 5000)
        String description
) {
}