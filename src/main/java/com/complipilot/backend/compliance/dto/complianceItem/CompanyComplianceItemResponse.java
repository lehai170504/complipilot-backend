package com.complipilot.backend.compliance.dto.complianceItem;

import java.time.LocalDate;
import java.util.UUID;

import com.complipilot.backend.compliance.enums.CompanyComplianceStatus;

public record CompanyComplianceItemResponse(
        UUID id,
        UUID organizationId,
        UUID requirementId,
        String requirementCode,
        String requirementTitle,
        CompanyComplianceStatus status,
        UUID ownerUserId,
        LocalDate dueDate,
        String notes
) {
}