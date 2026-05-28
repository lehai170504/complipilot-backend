package com.complipilot.backend.evidence.controller;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.common.pagination.PageResponse;
import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.evidence.dto.*;

import com.complipilot.backend.evidence.enums.EvidenceSourceType;
import com.complipilot.backend.evidence.enums.EvidenceType;
import com.complipilot.backend.evidence.service.EvidenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<PageResponse<EvidenceDocumentResponse>> listEvidenceDocuments(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) EvidenceType evidenceType,
            @RequestParam(required = false) EvidenceSourceType sourceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                evidenceService.listEvidenceDocuments(
                        organizationId,
                        authenticatedUser.id(),
                        new EvidenceFilterRequest(
                                evidenceType,
                                sourceType
                        ),
                        page,
                        size
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

    @Operation(
            summary = "Create presigned upload URL for evidence file",
            description = "Returns a presigned PUT URL and object key. The frontend uploads the file directly to object storage.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/organizations/{organizationId}/evidence/upload-url")
    public ResponseEntity<CreateEvidenceUploadUrlResponse> createUploadUrl(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateEvidenceUploadUrlRequest request
    ) {
        return ResponseEntity.ok(
                evidenceService.createUploadUrl(
                        organizationId,
                        authenticatedUser.id(),
                        request
                )
        );
    }

    @Operation(
            summary = "Create presigned download URL for file evidence",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/organizations/{organizationId}/evidence/{evidenceId}/download-url")
    public ResponseEntity<EvidenceDownloadUrlResponse> createDownloadUrl(
            @PathVariable UUID organizationId,
            @PathVariable UUID evidenceId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                evidenceService.createDownloadUrl(
                        organizationId,
                        evidenceId,
                        authenticatedUser.id()
                )
        );
    }
}