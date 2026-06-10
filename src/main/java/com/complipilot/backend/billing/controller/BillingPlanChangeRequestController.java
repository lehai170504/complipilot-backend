package com.complipilot.backend.billing.controller;

import java.util.UUID;

import com.complipilot.backend.billing.dto.BillingPlanChangeRequestResponse;
import com.complipilot.backend.billing.dto.CreateBillingPlanChangeRequest;
import com.complipilot.backend.billing.service.BillingPlanChangeRequestService;
import com.complipilot.backend.common.pagination.PageResponse;
import com.complipilot.backend.common.security.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Billing", description = "Workspace billing and plan change requests")
@RestController
public class BillingPlanChangeRequestController {

    private final BillingPlanChangeRequestService requestService;

    public BillingPlanChangeRequestController(
            BillingPlanChangeRequestService requestService
    ) {
        this.requestService = requestService;
    }

    @Operation(
            summary = "Create billing plan change request",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/organizations/{organizationId}/billing/plan-change-requests")
    public ResponseEntity<BillingPlanChangeRequestResponse> createRequest(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateBillingPlanChangeRequest request
    ) {
        return ResponseEntity.ok(
                requestService.createRequest(
                        organizationId,
                        authenticatedUser.id(),
                        request
                )
        );
    }

    @Operation(
            summary = "Get latest billing plan change request",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/billing/plan-change-requests/latest")
    public ResponseEntity<BillingPlanChangeRequestResponse> getLatestRequest(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                requestService.getLatestRequest(
                        organizationId,
                        authenticatedUser.id()
                )
        );
    }

    @Operation(
            summary = "List billing plan change requests",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/billing/plan-change-requests")
    public ResponseEntity<PageResponse<BillingPlanChangeRequestResponse>> listRequests(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                requestService.listWorkspaceRequests(
                        organizationId,
                        authenticatedUser.id(),
                        page,
                        size
                )
        );
    }
}