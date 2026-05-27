package com.complipilot.backend.evidence.dto;

import java.time.Instant;
import java.util.UUID;

import com.complipilot.backend.evidence.enums.EvidenceSourceType;
import com.complipilot.backend.evidence.enums.EvidenceStatus;
import com.complipilot.backend.evidence.enums.EvidenceType;

public record EvidenceDocumentResponse(
        UUID id,
        UUID organizationId,
        String title,
        String description,
        EvidenceType evidenceType,
        EvidenceSourceType sourceType,
        String fileObjectKey,
        String externalUrl,
        String contentType,
        Long fileSizeBytes,
        UUID uploadedByUserId,
        EvidenceStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}