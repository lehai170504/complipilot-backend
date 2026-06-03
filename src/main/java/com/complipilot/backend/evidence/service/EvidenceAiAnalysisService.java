package com.complipilot.backend.evidence.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.complipilot.backend.ai.dto.EvidenceAiAnalysisRequest;
import com.complipilot.backend.ai.dto.EvidenceAiAnalysisResponse;
import com.complipilot.backend.ai.service.AiEvidenceAnalysisClient;
import com.complipilot.backend.billing.service.UsageQuotaService;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.evidence.dto.EvidenceAiAnalysisRecordResponse;
import com.complipilot.backend.evidence.entity.ComplianceItemEvidenceLink;
import com.complipilot.backend.evidence.entity.EvidenceAiAnalysis;
import com.complipilot.backend.evidence.entity.EvidenceDocument;
import com.complipilot.backend.evidence.repository.ComplianceItemEvidenceLinkRepository;
import com.complipilot.backend.evidence.repository.EvidenceAiAnalysisRepository;
import com.complipilot.backend.evidence.repository.EvidenceDocumentRepository;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;
import com.complipilot.backend.organization.service.TenantAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceAiAnalysisService {

    private final EvidenceDocumentRepository evidenceDocumentRepository;
    private final ComplianceItemEvidenceLinkRepository evidenceLinkRepository;
    private final EvidenceAiAnalysisRepository evidenceAiAnalysisRepository;
    private final UserRepository userRepository;
    private final TenantAccessService tenantAccessService;
    private final AiEvidenceAnalysisClient aiEvidenceAnalysisClient;
    private final UsageQuotaService usageQuotaService;

    public EvidenceAiAnalysisService(
            EvidenceDocumentRepository evidenceDocumentRepository,
            ComplianceItemEvidenceLinkRepository evidenceLinkRepository,
            EvidenceAiAnalysisRepository evidenceAiAnalysisRepository,
            UserRepository userRepository,
            TenantAccessService tenantAccessService,
            AiEvidenceAnalysisClient aiEvidenceAnalysisClient,
            UsageQuotaService usageQuotaService
    ) {
        this.evidenceDocumentRepository = evidenceDocumentRepository;
        this.evidenceLinkRepository = evidenceLinkRepository;
        this.evidenceAiAnalysisRepository = evidenceAiAnalysisRepository;
        this.userRepository = userRepository;
        this.tenantAccessService = tenantAccessService;
        this.aiEvidenceAnalysisClient = aiEvidenceAnalysisClient;
        this.usageQuotaService = usageQuotaService;
    }

    @Transactional
    public EvidenceAiAnalysisRecordResponse analyzeAndSave(
            UUID organizationId,
            UUID evidenceId,
            UUID userId
    ) {
        tenantAccessService.requireActiveMember(organizationId, userId);
        usageQuotaService.requireCanRunAiAnalysis(organizationId);

        EvidenceDocument evidence = evidenceDocumentRepository
                .findByIdAndOrganization_Id(evidenceId, organizationId)
                .orElseThrow(() -> new NotFoundException("Evidence document not found"));

        User analyzedByUser = userRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

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

        EvidenceAiAnalysisResponse aiResponse =
                aiEvidenceAnalysisClient.analyzeEvidence(request);

        EvidenceAiAnalysis analysis = new EvidenceAiAnalysis(
                evidence.getOrganization(),
                evidence,
                analyzedByUser,
                aiResponse.summary(),
                aiResponse.riskLevel(),
                BigDecimal.valueOf(aiResponse.confidence()),
                aiResponse.findings(),
                aiResponse.missingInformation(),
                aiResponse.suggestedActions()
        );

        EvidenceAiAnalysis savedAnalysis =
                evidenceAiAnalysisRepository.save(analysis);

        usageQuotaService.recordAiAnalysisRun(organizationId);

        return toResponse(savedAnalysis);
    }

    @Transactional(readOnly = true)
    public EvidenceAiAnalysisRecordResponse getLatestAnalysis(
            UUID organizationId,
            UUID evidenceId,
            UUID userId
    ) {
        tenantAccessService.requireActiveMember(organizationId, userId);

        EvidenceAiAnalysis latestAnalysis = evidenceAiAnalysisRepository
                .findTopByOrganization_IdAndEvidenceDocument_IdOrderByAnalyzedAtDesc(
                        organizationId,
                        evidenceId
                )
                .orElseThrow(() -> new NotFoundException("Evidence AI analysis not found"));

        return toResponse(latestAnalysis);
    }

    @Transactional(readOnly = true)
    public List<EvidenceAiAnalysisRecordResponse> listAnalysisHistory(
            UUID organizationId,
            UUID evidenceId,
            UUID userId
    ) {
        tenantAccessService.requireActiveMember(organizationId, userId);

        return evidenceAiAnalysisRepository
                .findTop10ByOrganization_IdAndEvidenceDocument_IdOrderByAnalyzedAtDesc(
                        organizationId,
                        evidenceId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private EvidenceAiAnalysisRecordResponse toResponse(
            EvidenceAiAnalysis analysis
    ) {
        return new EvidenceAiAnalysisRecordResponse(
                analysis.getId(),
                analysis.getEvidenceDocument().getId(),
                analysis.getSummary(),
                analysis.getRiskLevel(),
                analysis.getConfidence().doubleValue(),
                analysis.getFindings(),
                analysis.getMissingInformation(),
                analysis.getSuggestedActions(),
                analysis.getAnalyzedByUser().getEmail(),
                analysis.getAnalyzedAt()
        );
    }
}