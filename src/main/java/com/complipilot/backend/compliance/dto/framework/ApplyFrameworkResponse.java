package com.complipilot.backend.compliance.dto.framework;

import com.complipilot.backend.compliance.dto.complianceItem.CompanyComplianceItemResponse;

import java.util.List;
import java.util.UUID;

public record ApplyFrameworkResponse(
        UUID organizationId,
        UUID frameworkId,
        int createdCount,
        int skippedCount,
        List<CompanyComplianceItemResponse> createdItems
) {
}