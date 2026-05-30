package com.complipilot.backend.evidence.controller;

import java.util.UUID;

import com.complipilot.backend.ai.dto.EvidenceAiAnalysisRequest;
import com.complipilot.backend.ai.dto.EvidenceAiAnalysisResponse;
import com.complipilot.backend.ai.service.AiEvidenceAnalysisClient;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.evidence.entity.ComplianceItemEvidenceLink;
import com.complipilot.backend.evidence.entity.EvidenceDocument;
import com.complipilot.backend.evidence.repository.ComplianceItemEvidenceLinkRepository;
import com.complipilot.backend.evidence.repository.EvidenceDocumentRepository;
import com.complipilot.backend.organization.service.TenantAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Evidence AI", description = "AI-assisted evidence analysis")
@RestController
public class EvidenceAiController {

    private final EvidenceDocumentRepository evidenceDocumentRepository;
    private final ComplianceItemEvidenceLinkRepository evidenceLinkRepository;
    private final TenantAccessService tenantAccessService;
    private final AiEvidenceAnalysisClient aiEvidenceAnalysisClient;

    public EvidenceAiController(
            EvidenceDocumentRepository evidenceDocumentRepository,
            ComplianceItemEvidenceLinkRepository evidenceLinkRepository,
            TenantAccessService tenantAccessService,
            AiEvidenceAnalysisClient aiEvidenceAnalysisClient
    ) {
        this.evidenceDocumentRepository = evidenceDocumentRepository;
        this.evidenceLinkRepository = evidenceLinkRepository;
        this.tenantAccessService = tenantAccessService;
        this.aiEvidenceAnalysisClient = aiEvidenceAnalysisClient;
    }

    @Operation(
            summary = "Analyze evidence with AI",
            description = "Runs AI analysis for an evidence document through the internal AI service.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Transactional(readOnly = true)
    @PostMapping("/api/v1/organizations/{organizationId}/evidence/{evidenceId}/ai/analyze")
    public EvidenceAiAnalysisResponse analyzeEvidence(
            @PathVariable UUID organizationId,
            @PathVariable UUID evidenceId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        tenantAccessService.requireActiveMember(
                organizationId,
                authenticatedUser.id()
        );

        EvidenceDocument evidence = evidenceDocumentRepository
                .findByIdAndOrganization_Id(evidenceId, organizationId)
                .orElseThrow(() -> new NotFoundException("Evidence document not found"));

        ComplianceItemEvidenceLink evidenceLink = evidenceLinkRepository
                .findFirstByComplianceItem_Organization_IdAndEvidenceDocument_Id(
                        organizationId,
                        evidenceId
                )
                .orElse(null);

        String linkedRequirementCode = null;
        String linkedRequirementTitle = null;

        if (evidenceLink != null) {
            linkedRequirementCode = evidenceLink
                    .getComplianceItem()
                    .getRequirement()
                    .getCode();

            linkedRequirementTitle = evidenceLink
                    .getComplianceItem()
                    .getRequirement()
                    .getTitle();
        }

        EvidenceAiAnalysisRequest request = new EvidenceAiAnalysisRequest(
                organizationId,
                evidence.getId(),
                evidence.getTitle(),
                evidence.getDescription(),
                evidence.getEvidenceType().name(),
                evidence.getSourceType().name(),
                evidence.getContentType(),
                evidence.getFileSizeBytes(),
                evidence.getExternalUrl(),
                linkedRequirementCode,
                linkedRequirementTitle
        );

        return aiEvidenceAnalysisClient.analyzeEvidence(request);
    }
}