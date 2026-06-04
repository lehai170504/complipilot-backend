package com.complipilot.backend.platform.controller;

import java.util.UUID;

import com.complipilot.backend.billing.dto.OrganizationUsageResponse;
import com.complipilot.backend.common.pagination.PageResponse;
import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.platform.dto.PlatformOrganizationResponse;
import com.complipilot.backend.platform.dto.PlatformUserResponse;
import com.complipilot.backend.platform.service.PlatformAdminApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Platform Admin", description = "Internal SaaS platform administration APIs")
@RestController
public class PlatformAdminController {

    private final PlatformAdminApiService platformAdminApiService;

    public PlatformAdminController(
            PlatformAdminApiService platformAdminApiService
    ) {
        this.platformAdminApiService = platformAdminApiService;
    }

    @Operation(
            summary = "List all organizations",
            description = "Platform-admin only. Returns organizations with subscription and usage overview.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/platform/organizations")
    public ResponseEntity<PageResponse<PlatformOrganizationResponse>> listOrganizations(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                platformAdminApiService.listOrganizations(
                        authenticatedUser,
                        page,
                        size
                )
        );
    }

    @Operation(
            summary = "List all users",
            description = "Platform-admin only. Returns users across the SaaS platform.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/platform/users")
    public ResponseEntity<PageResponse<PlatformUserResponse>> listUsers(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                platformAdminApiService.listUsers(
                        authenticatedUser,
                        page,
                        size
                )
        );
    }

    @Operation(
            summary = "Get organization usage",
            description = "Platform-admin only. Returns plan and usage limits for an organization.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/platform/organizations/{organizationId}/usage")
    public ResponseEntity<OrganizationUsageResponse> getOrganizationUsage(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID organizationId
    ) {
        return ResponseEntity.ok(
                platformAdminApiService.getOrganizationUsage(
                        authenticatedUser,
                        organizationId
                )
        );
    }
}