package com.complipilot.backend.billing.controller;

import java.util.UUID;

import com.complipilot.backend.billing.dto.OrganizationUsageResponse;
import com.complipilot.backend.billing.service.UsageQuotaService;
import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.organization.service.TenantAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Billing", description = "Subscription plan and usage quota APIs")
@RestController
public class BillingController {

    private final UsageQuotaService usageQuotaService;
    private final TenantAccessService tenantAccessService;

    public BillingController(
            UsageQuotaService usageQuotaService,
            TenantAccessService tenantAccessService
    ) {
        this.usageQuotaService = usageQuotaService;
        this.tenantAccessService = tenantAccessService;
    }

    @Operation(
            summary = "Get organization usage and plan limits",
            description = "Returns the current subscription plan and usage counters for an organization.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/billing/usage")
    public ResponseEntity<OrganizationUsageResponse> getOrganizationUsage(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        tenantAccessService.requireActiveMember(
                organizationId,
                authenticatedUser.id()
        );

        return ResponseEntity.ok(
                usageQuotaService.getOrganizationUsage(organizationId)
        );
    }
}