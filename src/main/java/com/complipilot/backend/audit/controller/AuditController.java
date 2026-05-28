package com.complipilot.backend.audit.controller;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.audit.dto.AuditEventFilterRequest;
import com.complipilot.backend.audit.dto.AuditEventResponse;
import com.complipilot.backend.audit.enums.AuditAction;
import com.complipilot.backend.audit.enums.AuditResourceType;
import com.complipilot.backend.audit.service.AuditService;
import com.complipilot.backend.common.pagination.PageResponse;
import com.complipilot.backend.common.security.AuthenticatedUser;

import com.complipilot.backend.common.sorting.SortDirection;
import com.complipilot.backend.common.sorting.SortRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit Trail", description = "Organization activity and audit events")
@RestController
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(
            summary = "List audit events",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/audit-events")
    public ResponseEntity<PageResponse<AuditEventResponse>> listAuditEvents(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditResourceType resourceType,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "DESC") SortDirection sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                auditService.listAuditEventsPage(
                        organizationId,
                        authenticatedUser.id(),
                        new AuditEventFilterRequest(
                                action,
                                resourceType,
                                query
                        ),
                        new SortRequest(
                                sortBy,
                                sortDirection
                        ),
                        page,
                        size
                )
        );
    }
}