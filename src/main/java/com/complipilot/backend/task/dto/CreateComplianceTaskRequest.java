package com.complipilot.backend.task.dto;

import com.complipilot.backend.task.enums.ComplianceTaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateComplianceTaskRequest(
        UUID complianceItemId,

        @NotBlank
        @Size(max = 250)
        String title,

        @Size(max = 10000)
        String description,

        UUID assigneeUserId,

        ComplianceTaskPriority priority,

        LocalDate dueDate
) {
}
