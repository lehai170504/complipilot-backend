package com.complipilot.backend.ai.dto;

import java.util.List;

public record ComplianceEvidenceSuggestionResponse(
        String summary,
        String coverageLevel,
        String riskLevel,
        Double confidence,
        List<String> existingEvidenceAssessment,
        List<String> missingEvidence,
        List<String> suggestedActions,
        String reviewerNote
) {
}