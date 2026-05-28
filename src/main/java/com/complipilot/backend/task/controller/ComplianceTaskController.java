package com.complipilot.backend.task.controller;

import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.task.dto.ComplianceTaskResponse;
import com.complipilot.backend.task.dto.ComplianceTaskSummaryResponse;
import com.complipilot.backend.task.dto.CreateComplianceTaskRequest;
import com.complipilot.backend.task.dto.UpdateComplianceTaskRequest;
import com.complipilot.backend.task.service.ComplianceTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Compliance Tasks", description = "Task tracking for compliance operations")
@RestController
public class ComplianceTaskController {

    private final ComplianceTaskService complianceTaskService;

    public ComplianceTaskController(ComplianceTaskService complianceTaskService) {
        this.complianceTaskService = complianceTaskService;
    }

    @Operation(
            summary = "Create compliance task",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/organizations/{organizationId}/tasks")
    public ResponseEntity<ComplianceTaskResponse> createTask(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateComplianceTaskRequest request
    ) {
        ComplianceTaskResponse response = complianceTaskService.createTask(
                organizationId,
                authenticatedUser.id(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List compliance tasks",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/tasks")
    public ResponseEntity<List<ComplianceTaskResponse>> listTasks(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                complianceTaskService.listTasks(
                        organizationId,
                        authenticatedUser.id()
                )
        );
    }

    @Operation(
            summary = "Get compliance task summary",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/tasks/summary")
    public ResponseEntity<ComplianceTaskSummaryResponse> getTaskSummary(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                complianceTaskService.getTaskSummary(
                        organizationId,
                        authenticatedUser.id()
                )
        );
    }

    @Operation(
            summary = "Update compliance task",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/api/v1/organizations/{organizationId}/tasks/{taskId}")
    public ResponseEntity<ComplianceTaskResponse> updateTask(
            @PathVariable UUID organizationId,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateComplianceTaskRequest request
    ) {
        return ResponseEntity.ok(
                complianceTaskService.updateTask(
                        organizationId,
                        taskId,
                        authenticatedUser.id(),
                        request
                )
        );
    }

    @Operation(
            summary = "Delete compliance task",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/api/v1/organizations/{organizationId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID organizationId,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        complianceTaskService.deleteTask(
                organizationId,
                taskId,
                authenticatedUser.id()
        );

        return ResponseEntity.noContent().build();
    }
}
