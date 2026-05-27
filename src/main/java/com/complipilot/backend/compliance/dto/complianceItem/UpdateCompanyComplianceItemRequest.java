package com.complipilot.backend.compliance.dto.complianceItem;

import java.time.LocalDate;
import java.util.UUID;

import com.complipilot.backend.compliance.enums.CompanyComplianceStatus;

public record UpdateCompanyComplianceItemRequest(
        CompanyComplianceStatus status,
        UUID ownerUserId,
        LocalDate dueDate,
        String notes
) {
}