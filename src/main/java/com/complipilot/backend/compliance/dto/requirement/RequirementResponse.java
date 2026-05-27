package com.complipilot.backend.compliance.dto.requirement;

import java.util.UUID;

public record RequirementResponse(
        UUID id,
        UUID frameworkId,
        String code,
        String title,
        String description,
        String category,
        int sortOrder
) {
}