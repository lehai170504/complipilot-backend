package com.complipilot.backend.organization.controller;

import java.util.UUID;

import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.organization.dto.OrganizationSettingsResponse;
import com.complipilot.backend.organization.dto.UpdateOrganizationSettingsRequest;
import com.complipilot.backend.organization.service.OrganizationSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Organization Settings", description = "Workspace settings APIs")
@RestController
public class OrganizationSettingsController {

    private final OrganizationSettingsService organizationSettingsService;

    public OrganizationSettingsController(
            OrganizationSettingsService organizationSettingsService
    ) {
        this.organizationSettingsService = organizationSettingsService;
    }

    @Operation(
            summary = "Get organization settings",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/settings")
    public ResponseEntity<OrganizationSettingsResponse> getSettings(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                organizationSettingsService.getSettings(
                        organizationId,
                        authenticatedUser.id()
                )
        );
    }

    @Operation(
            summary = "Update organization settings",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/api/v1/organizations/{organizationId}/settings")
    public ResponseEntity<OrganizationSettingsResponse> updateSettings(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateOrganizationSettingsRequest request
    ) {
        return ResponseEntity.ok(
                organizationSettingsService.updateSettings(
                        organizationId,
                        authenticatedUser.id(),
                        request
                )
        );
    }
}