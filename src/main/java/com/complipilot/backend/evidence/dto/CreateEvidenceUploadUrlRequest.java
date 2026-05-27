package com.complipilot.backend.evidence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEvidenceUploadUrlRequest(
        @NotBlank
        @Size(max = 255)
        String filename,

        @NotBlank
        @Size(max = 150)
        String contentType
) {
}