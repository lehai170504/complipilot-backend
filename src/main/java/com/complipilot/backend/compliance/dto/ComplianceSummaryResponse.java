package com.complipilot.backend.compliance.dto;

import java.util.UUID;

public record ComplianceSummaryResponse(
        UUID organizationId,
        long totalItems,
        long open,
        long inProgress,
        long readyForReview,
        long compliant,
        long nonCompliant,
        long waived
) {
}