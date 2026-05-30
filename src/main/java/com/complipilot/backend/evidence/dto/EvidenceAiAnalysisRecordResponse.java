package com.complipilot.backend.evidence.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EvidenceAiAnalysisRecordResponse(
        UUID id,
        UUID evidenceId,
        String summary,
        String riskLevel,
        Double confidence,
        List<String> findings,
        List<String> missingInformation,
        List<String> suggestedActions,
        String analyzedByEmail,
        Instant analyzedAt
) {
}