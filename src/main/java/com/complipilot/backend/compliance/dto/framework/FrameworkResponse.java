package com.complipilot.backend.compliance.dto.framework;

import java.util.UUID;

public record FrameworkResponse(
        UUID id,
        String code,
        String name,
        String description,
        boolean systemTemplate
) {
}