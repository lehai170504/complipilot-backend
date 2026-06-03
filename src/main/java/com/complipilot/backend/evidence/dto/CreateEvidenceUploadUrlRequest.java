package com.complipilot.backend.evidence.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEvidenceUploadUrlRequest(
        @NotBlank(message = "filename is required")
        String filename,

        @NotBlank(message = "contentType is required")
        String contentType,

        @NotNull(message = "fileSizeBytes is required")
        @Min(value = 1, message = "fileSizeBytes must be greater than 0")
        Long fileSizeBytes
) {
}