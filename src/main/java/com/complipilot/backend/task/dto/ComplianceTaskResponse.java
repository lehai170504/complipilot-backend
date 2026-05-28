package com.complipilot.backend.task.dto;


import com.complipilot.backend.task.enums.ComplianceTaskPriority;
import com.complipilot.backend.task.enums.ComplianceTaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ComplianceTaskResponse(
        UUID id,
        UUID organizationId,
        UUID complianceItemId,
        String title,
        String description,
        UUID assigneeUserId,
        String assigneeEmail,
        UUID createdByUserId,
        String createdByEmail,
        ComplianceTaskStatus status,
        ComplianceTaskPriority priority,
        LocalDate dueDate,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}