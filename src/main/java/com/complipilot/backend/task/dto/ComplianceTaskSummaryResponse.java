package com.complipilot.backend.task.dto;

import java.util.UUID;

public record ComplianceTaskSummaryResponse(
        UUID organizationId,
        long total,
        long open,
        long inProgress,
        long done,
        long cancelled,
        long overdue
) {
}