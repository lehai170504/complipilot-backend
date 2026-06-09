package com.complipilot.backend.identity.controller;

import com.complipilot.backend.audit.dto.AuditEventResponse;
import com.complipilot.backend.audit.service.AuditService;
import com.complipilot.backend.common.pagination.PageResponse;
import com.complipilot.backend.common.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Profile", description = "Authenticated user profile APIs")
@RestController
public class ProfileActivityController {

    private final AuditService auditService;

    public ProfileActivityController(AuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(
            summary = "List current user activity",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/profile/activity")
    public ResponseEntity<PageResponse<AuditEventResponse>> listCurrentUserActivity(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                auditService.listCurrentUserActivity(
                        authenticatedUser.id(),
                        page,
                        size
                )
        );
    }
}