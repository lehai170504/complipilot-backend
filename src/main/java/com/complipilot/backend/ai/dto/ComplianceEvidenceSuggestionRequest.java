package com.complipilot.backend.ai.dto;

import java.util.List;
import java.util.UUID;

public record ComplianceEvidenceSuggestionRequest(
        UUID organizationId,
        UUID complianceItemId,
        String requirementCode,
        String requirementTitle,
        String requirementDescription,
        String complianceStatus,
        String notes,
        List<LinkedEvidenceAiItem> linkedEvidence
) {
}