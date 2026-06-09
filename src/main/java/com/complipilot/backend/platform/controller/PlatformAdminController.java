package com.complipilot.backend.platform.controller;

import java.util.UUID;

import com.complipilot.backend.billing.dto.BillingPlanChangeRequestResponse;
import com.complipilot.backend.billing.dto.OrganizationUsageResponse;
import com.complipilot.backend.billing.enums.BillingPlanChangeRequestStatus;
import com.complipilot.backend.billing.service.BillingPlanChangeRequestService;
import com.complipilot.backend.common.pagination.PageResponse;
import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.platform.dto.PlatformOrganizationResponse;
import com.complipilot.backend.platform.dto.PlatformUserResponse;
import com.complipilot.backend.platform.dto.UpdateOrganizationSubscriptionRequest;
import com.complipilot.backend.platform.service.PlatformAdminApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Platform Admin", description = "Internal SaaS platform administration APIs")
@RestController
public class PlatformAdminController {

    private final PlatformAdminApiService platformAdminApiService;
    private final BillingPlanChangeRequestService billingPlanChangeRequestService;

    public PlatformAdminController(
            PlatformAdminApiService platformAdminApiService,
            BillingPlanChangeRequestService billingPlanChangeRequestService
    ) {
        this.platformAdminApiService = platformAdminApiService;
        this.billingPlanChangeRequestService = billingPlanChangeRequestService;
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

    @Operation(
            summary = "Update organization subscription plan",
            description = "Platform-admin only. Manually changes an organization's plan.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/api/v1/platform/organizations/{organizationId}/subscription")
    public ResponseEntity<OrganizationUsageResponse> updateOrganizationSubscription(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID organizationId,
            @Valid @RequestBody UpdateOrganizationSubscriptionRequest request
    ) {
        return ResponseEntity.ok(
                platformAdminApiService.updateOrganizationSubscription(
                        authenticatedUser,
                        organizationId,
                        request
                )
        );
    }

    @Operation(
            summary = "List billing plan change requests",
            description = "Platform-admin only. Returns billing upgrade/downgrade requests.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/platform/billing/plan-change-requests")
    public ResponseEntity<PageResponse<BillingPlanChangeRequestResponse>> listPlanChangeRequests(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) BillingPlanChangeRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                billingPlanChangeRequestService.listPlatformRequests(
                        authenticatedUser,
                        status,
                        page,
                        size
                )
        );
    }

    @Operation(
            summary = "Approve billing plan change request",
            description = "Platform-admin only. Approves a pending request and updates organization plan.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/api/v1/platform/billing/plan-change-requests/{requestId}/approve")
    public ResponseEntity<BillingPlanChangeRequestResponse> approvePlanChangeRequest(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID requestId
    ) {
        return ResponseEntity.ok(
                billingPlanChangeRequestService.approveRequest(
                        authenticatedUser,
                        requestId
                )
        );
    }

    @Operation(
            summary = "Reject billing plan change request",
            description = "Platform-admin only. Rejects a pending billing request.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/api/v1/platform/billing/plan-change-requests/{requestId}/reject")
    public ResponseEntity<BillingPlanChangeRequestResponse> rejectPlanChangeRequest(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID requestId
    ) {
        return ResponseEntity.ok(
                billingPlanChangeRequestService.rejectRequest(
                        authenticatedUser,
                        requestId
                )
        );
    }
}