package com.complipilot.backend.evidence.dto;

import com.complipilot.backend.evidence.enums.EvidenceSourceType;
import com.complipilot.backend.evidence.enums.EvidenceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEvidenceDocumentRequest(
        @NotBlank
        @Size(max = 250)
        String title,

        @Size(max = 10000)
        String description,

        @NotNull
        EvidenceType evidenceType,

        @NotNull
        EvidenceSourceType sourceType,

        @Size(max = 500)
        String fileObjectKey,

        @Size(max = 1000)
        String externalUrl,

        @Size(max = 150)
        String contentType,

        Long fileSizeBytes
) {
}