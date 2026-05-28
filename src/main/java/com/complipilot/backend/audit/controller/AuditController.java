package com.complipilot.backend.audit.controller;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.audit.dto.AuditEventResponse;
import com.complipilot.backend.audit.service.AuditService;
import com.complipilot.backend.common.security.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit Trail", description = "Organization activity and audit events")
@RestController
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(
            summary = "List recent organization audit events",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/audit-events")
    public ResponseEntity<List<AuditEventResponse>> listRecentEvents(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                auditService.listRecentEvents(
                        organizationId,
                        authenticatedUser.id()
                )
        );
    }
}