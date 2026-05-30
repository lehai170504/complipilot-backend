package com.complipilot.backend.compliance.controller;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.ai.dto.ComplianceEvidenceSuggestionRequest;
import com.complipilot.backend.ai.dto.ComplianceEvidenceSuggestionResponse;
import com.complipilot.backend.ai.dto.LinkedEvidenceAiItem;
import com.complipilot.backend.ai.service.AiEvidenceAnalysisClient;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.compliance.entity.CompanyComplianceItem;
import com.complipilot.backend.compliance.repository.CompanyComplianceItemRepository;
import com.complipilot.backend.evidence.entity.ComplianceItemEvidenceLink;
import com.complipilot.backend.evidence.repository.ComplianceItemEvidenceLinkRepository;
import com.complipilot.backend.organization.service.TenantAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Compliance AI", description = "AI-assisted compliance item review")
@RestController
public class ComplianceAiController {

    private final CompanyComplianceItemRepository complianceItemRepository;
    private final ComplianceItemEvidenceLinkRepository evidenceLinkRepository;
    private final TenantAccessService tenantAccessService;
    private final AiEvidenceAnalysisClient aiEvidenceAnalysisClient;

    public ComplianceAiController(
            CompanyComplianceItemRepository complianceItemRepository,
            ComplianceItemEvidenceLinkRepository evidenceLinkRepository,
            TenantAccessService tenantAccessService,
            AiEvidenceAnalysisClient aiEvidenceAnalysisClient
    ) {
        this.complianceItemRepository = complianceItemRepository;
        this.evidenceLinkRepository = evidenceLinkRepository;
        this.tenantAccessService = tenantAccessService;
        this.aiEvidenceAnalysisClient = aiEvidenceAnalysisClient;
    }

    @Operation(
            summary = "Suggest missing evidence with AI",
            description = "Reviews a compliance item and its linked evidence, then suggests missing evidence and next actions.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Transactional(readOnly = true)
    @PostMapping("/api/v1/organizations/{organizationId}/compliance-items/{itemId}/ai/suggest-evidence")
    public ComplianceEvidenceSuggestionResponse suggestEvidence(
            @PathVariable UUID organizationId,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        tenantAccessService.requireActiveMember(
                organizationId,
                authenticatedUser.id()
        );

        CompanyComplianceItem item = complianceItemRepository
                .findByIdAndOrganization_Id(itemId, organizationId)
                .orElseThrow(() -> new NotFoundException("Compliance item not found"));

        List<ComplianceItemEvidenceLink> evidenceLinks =
                evidenceLinkRepository.findByComplianceItem_Id(itemId);

        List<LinkedEvidenceAiItem> linkedEvidence = evidenceLinks.stream()
                .map(link -> new LinkedEvidenceAiItem(
                        link.getEvidenceDocument().getId(),
                        link.getEvidenceDocument().getTitle(),
                        link.getEvidenceDocument().getDescription(),
                        link.getEvidenceDocument().getEvidenceType().name(),
                        link.getEvidenceDocument().getSourceType().name(),
                        link.getEvidenceDocument().getContentType(),
                        link.getEvidenceDocument().getExternalUrl()
                ))
                .toList();

        ComplianceEvidenceSuggestionRequest request =
                new ComplianceEvidenceSuggestionRequest(
                        organizationId,
                        item.getId(),
                        item.getRequirement().getCode(),
                        item.getRequirement().getTitle(),
                        item.getRequirement().getDescription(),
                        item.getStatus().name(),
                        item.getNotes(),
                        linkedEvidence
                );

        return aiEvidenceAnalysisClient.suggestComplianceEvidence(request);
    }
}