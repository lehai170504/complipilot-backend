package com.complipilot.backend.evidence.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record LinkEvidenceRequest(
        @NotNull
        UUID evidenceDocumentId
) {
}