package com.complipilot.backend.compliance.controller;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.compliance.dto.ComplianceSummaryResponse;
import com.complipilot.backend.compliance.dto.framework.ApplyFrameworkResponse;
import com.complipilot.backend.compliance.dto.complianceItem.CompanyComplianceItemResponse;
import com.complipilot.backend.compliance.dto.complianceItem.CreateCompanyComplianceItemRequest;
import com.complipilot.backend.compliance.dto.framework.CreateFrameworkRequest;
import com.complipilot.backend.compliance.dto.requirement.CreateRequirementRequest;
import com.complipilot.backend.compliance.dto.framework.FrameworkResponse;
import com.complipilot.backend.compliance.dto.requirement.RequirementResponse;
import com.complipilot.backend.compliance.dto.complianceItem.UpdateCompanyComplianceItemRequest;

import com.complipilot.backend.compliance.service.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Compliance", description = "Compliance frameworks, requirements, and company compliance items")
@RestController
public class ComplianceController {

    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @Operation(
            summary = "Create compliance framework",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/compliance/frameworks")
    public ResponseEntity<FrameworkResponse> createFramework(
            @Valid @RequestBody CreateFrameworkRequest request
    ) {
        FrameworkResponse response = complianceService.createFramework(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List compliance frameworks",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/compliance/frameworks")
    public ResponseEntity<List<FrameworkResponse>> listFrameworks() {
        return ResponseEntity.ok(complianceService.listFrameworks());
    }

    @Operation(
            summary = "Create requirement under a framework",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/compliance/frameworks/{frameworkId}/requirements")
    public ResponseEntity<RequirementResponse> createRequirement(
            @PathVariable UUID frameworkId,
            @Valid @RequestBody CreateRequirementRequest request
    ) {
        RequirementResponse response = complianceService.createRequirement(
                frameworkId,
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List requirements under a framework",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/compliance/frameworks/{frameworkId}/requirements")
    public ResponseEntity<List<RequirementResponse>> listRequirements(
            @PathVariable UUID frameworkId
    ) {
        return ResponseEntity.ok(complianceService.listRequirements(frameworkId));
    }

    @Operation(
            summary = "Seed SME Security Baseline template",
            description = "Creates a starter compliance framework and baseline requirements for local development/demo.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/compliance/frameworks/seed/security-baseline")
    public ResponseEntity<FrameworkResponse> seedSecurityBaselineTemplate() {
        FrameworkResponse response = complianceService.seedSecurityBaselineTemplate();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Apply framework to organization",
            description = "Creates company compliance items for all requirements in the selected framework. Existing items are skipped.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/organizations/{organizationId}/compliance-frameworks/{frameworkId}/apply")
    public ResponseEntity<ApplyFrameworkResponse> applyFrameworkToOrganization(
            @PathVariable UUID organizationId,
            @PathVariable UUID frameworkId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        ApplyFrameworkResponse response = complianceService.applyFrameworkToOrganization(
                organizationId,
                frameworkId,
                authenticatedUser.id()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Create company compliance item",
            description = "Creates a compliance tracking item for an organization and a requirement.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/organizations/{organizationId}/compliance-items")
    public ResponseEntity<CompanyComplianceItemResponse> createCompanyComplianceItem(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateCompanyComplianceItemRequest request
    ) {
        CompanyComplianceItemResponse response = complianceService.createCompanyComplianceItem(
                organizationId,
                authenticatedUser.id(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List organization compliance items",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/compliance-items")
    public ResponseEntity<List<CompanyComplianceItemResponse>> listCompanyComplianceItems(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                complianceService.listCompanyComplianceItems(
                        organizationId,
                        authenticatedUser.id()
                )
        );
    }

    @Operation(
            summary = "Update company compliance item",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/api/v1/organizations/{organizationId}/compliance-items/{itemId}")
    public ResponseEntity<CompanyComplianceItemResponse> updateCompanyComplianceItem(
            @PathVariable UUID organizationId,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody UpdateCompanyComplianceItemRequest request
    ) {
        return ResponseEntity.ok(
                complianceService.updateCompanyComplianceItem(
                        organizationId,
                        itemId,
                        authenticatedUser.id(),
                        request
                )
        );
    }

    @Operation(
            summary = "Get organization compliance summary",
            description = "Returns compliance item counts grouped by status for the organization.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/compliance-summary")
    public ResponseEntity<ComplianceSummaryResponse> getComplianceSummary(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                complianceService.getComplianceSummary(
                        organizationId,
                        authenticatedUser.id()
                )
        );
    }
}