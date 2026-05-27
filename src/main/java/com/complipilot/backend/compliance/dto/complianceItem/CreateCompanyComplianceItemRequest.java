package com.complipilot.backend.compliance.dto.complianceItem;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateCompanyComplianceItemRequest(
        @NotNull
        UUID requirementId
) {
}