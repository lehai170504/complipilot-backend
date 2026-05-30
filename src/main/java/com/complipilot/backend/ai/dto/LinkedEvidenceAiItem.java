package com.complipilot.backend.ai.dto;

import java.util.UUID;

public record LinkedEvidenceAiItem(
        UUID evidenceId,
        String title,
        String description,
        String evidenceType,
        String sourceType,
        String contentType,
        String externalUrl
) {
}