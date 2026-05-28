package com.complipilot.backend.task.dto;

import java.util.UUID;

import com.complipilot.backend.task.enums.ComplianceTaskPriority;
import com.complipilot.backend.task.enums.ComplianceTaskStatus;

public record ComplianceTaskFilterRequest(
        ComplianceTaskStatus status,
        ComplianceTaskPriority priority,
        UUID assigneeUserId,
        UUID complianceItemId,
        String query
) {
}