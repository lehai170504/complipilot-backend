package com.complipilot.backend.evidence.controller;

import java.util.UUID;

import com.complipilot.backend.ai.dto.EvidenceAiAnalysisRequest;
import com.complipilot.backend.ai.dto.EvidenceAiAnalysisResponse;
import com.complipilot.backend.ai.service.AiEvidenceAnalysisClient;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.evidence.entity.EvidenceDocument;
import com.complipilot.backend.evidence.repository.EvidenceDocumentRepository;
import com.complipilot.backend.organization.service.TenantAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Evidence AI", description = "AI-assisted evidence analysis")
@RestController
public class EvidenceAiController {

    private static final Logger log = LoggerFactory.getLogger(EvidenceAiController.class);

    private final EvidenceDocumentRepository evidenceDocumentRepository;
    private final TenantAccessService tenantAccessService;
    private final AiEvidenceAnalysisClient aiEvidenceAnalysisClient;

    public EvidenceAiController(
            EvidenceDocumentRepository evidenceDocumentRepository,
            TenantAccessService tenantAccessService,
            AiEvidenceAnalysisClient aiEvidenceAnalysisClient
    ) {
        this.evidenceDocumentRepository = evidenceDocumentRepository;
        this.tenantAccessService = tenantAccessService;
        this.aiEvidenceAnalysisClient = aiEvidenceAnalysisClient;
    }

    @Operation(
            summary = "Analyze evidence with AI",
            description = "Runs AI analysis for an evidence document through the internal AI service.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/organizations/{organizationId}/evidence/{evidenceId}/ai/analyze")
    public EvidenceAiAnalysisResponse analyzeEvidence(
            @PathVariable UUID organizationId,
            @PathVariable UUID evidenceId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        log.info(
                "AI evidence analyze requested. organizationId={}, evidenceId={}, userId={}",
                organizationId,
                evidenceId,
                authenticatedUser.id()
        );

        tenantAccessService.requireActiveMember(organizationId, authenticatedUser.id());

        EvidenceDocument evidence = evidenceDocumentRepository
                .findByIdAndOrganization_Id(evidenceId, organizationId)
                .orElseThrow(() -> new NotFoundException("Evidence document not found"));

        log.info(
                "Evidence loaded for AI analysis. evidenceId={}, title={}, evidenceType={}, sourceType={}",
                evidence.getId(),
                evidence.getTitle(),
                evidence.getEvidenceType(),
                evidence.getSourceType()
        );

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
                null,
                null
        );

        log.info("Sending evidence analysis request to AI service. request={}", request);

        EvidenceAiAnalysisResponse response = aiEvidenceAnalysisClient.analyzeEvidence(request);

        log.info(
                "AI evidence analysis completed. evidenceId={}, riskLevel={}, confidence={}",
                evidenceId,
                response.riskLevel(),
                response.confidence()
        );

        return response;
    }
}