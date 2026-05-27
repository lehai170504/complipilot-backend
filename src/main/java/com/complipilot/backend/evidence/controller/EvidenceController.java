package com.complipilot.backend.evidence.controller;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.evidence.dto.ComplianceItemEvidenceResponse;
import com.complipilot.backend.evidence.dto.CreateEvidenceDocumentRequest;
import com.complipilot.backend.evidence.dto.EvidenceDocumentResponse;
import com.complipilot.backend.evidence.dto.LinkEvidenceRequest;
import com.complipilot.backend.evidence.dto.UpdateEvidenceDocumentRequest;

import com.complipilot.backend.evidence.service.EvidenceService;
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

@Tag(name = "Evidence", description = "Evidence document metadata and compliance item evidence links")
@RestController
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @Operation(
            summary = "Create evidence document metadata",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/organizations/{organizationId}/evidence")
    public ResponseEntity<EvidenceDocumentResponse> createEvidenceDocument(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateEvidenceDocumentRequest request
    ) {
        EvidenceDocumentResponse response = evidenceService.createEvidenceDocument(
                organizationId,
                authenticatedUser.id(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List active evidence documents for organization",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/evidence")
    public ResponseEntity<List<EvidenceDocumentResponse>> listEvidenceDocuments(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                evidenceService.listEvidenceDocuments(
                        organizationId,
                        authenticatedUser.id()
                )
        );
    }

    @Operation(
            summary = "Update evidence document metadata",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/api/v1/organizations/{organizationId}/evidence/{evidenceId}")
    public ResponseEntity<EvidenceDocumentResponse> updateEvidenceDocument(
            @PathVariable UUID organizationId,
            @PathVariable UUID evidenceId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateEvidenceDocumentRequest request
    ) {
        return ResponseEntity.ok(
                evidenceService.updateEvidenceDocument(
                        organizationId,
                        evidenceId,
                        authenticatedUser.id(),
                        request
                )
        );
    }

    @Operation(
            summary = "Archive evidence document",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/api/v1/organizations/{organizationId}/evidence/{evidenceId}")
    public ResponseEntity<Void> archiveEvidenceDocument(
            @PathVariable UUID organizationId,
            @PathVariable UUID evidenceId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        evidenceService.archiveEvidenceDocument(
                organizationId,
                evidenceId,
                authenticatedUser.id()
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Link evidence to compliance item",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/organizations/{organizationId}/compliance-items/{itemId}/evidence-links")
    public ResponseEntity<ComplianceItemEvidenceResponse> linkEvidenceToComplianceItem(
            @PathVariable UUID organizationId,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody LinkEvidenceRequest request
    ) {
        ComplianceItemEvidenceResponse response = evidenceService.linkEvidenceToComplianceItem(
                organizationId,
                itemId,
                authenticatedUser.id(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List evidence linked to compliance item",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/compliance-items/{itemId}/evidence-links")
    public ResponseEntity<List<ComplianceItemEvidenceResponse>> listEvidenceLinksForComplianceItem(
            @PathVariable UUID organizationId,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                evidenceService.listEvidenceLinksForComplianceItem(
                        organizationId,
                        itemId,
                        authenticatedUser.id()
                )
        );
    }

    @Operation(
            summary = "Unlink evidence from compliance item",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/api/v1/organizations/{organizationId}/compliance-items/{itemId}/evidence-links/{evidenceDocumentId}")
    public ResponseEntity<Void> unlinkEvidenceFromComplianceItem(
            @PathVariable UUID organizationId,
            @PathVariable UUID itemId,
            @PathVariable UUID evidenceDocumentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        evidenceService.unlinkEvidenceFromComplianceItem(
                organizationId,
                itemId,
                evidenceDocumentId,
                authenticatedUser.id()
        );

        return ResponseEntity.noContent().build();
    }
}