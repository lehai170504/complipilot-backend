package com.complipilot.backend.ai.dto;

import java.util.UUID;

public record EvidenceAiAnalysisRequest(
        UUID organizationId,
        UUID evidenceId,
        String title,
        String description,
        String evidenceType,
        String sourceType,
        String contentType,
        Long fileSizeBytes,
        String externalUrl,
        String linkedRequirementCode,
        String linkedRequirementTitle
) {
}