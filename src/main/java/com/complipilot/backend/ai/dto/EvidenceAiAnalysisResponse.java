package com.complipilot.backend.ai.dto;

import java.util.List;

public record EvidenceAiAnalysisResponse(
        String summary,
        String riskLevel,
        Double confidence,
        List<String> findings,
        List<String> missingInformation,
        List<String> suggestedActions
) {
}