package com.complipilot.backend.evidence.dto;

import com.complipilot.backend.evidence.enums.EvidenceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateEvidenceDocumentRequest(
        @NotBlank
        @Size(max = 250)
        String title,

        @Size(max = 10000)
        String description,

        @NotNull
        EvidenceType evidenceType,

        @Size(max = 1000)
        String externalUrl
) {
}