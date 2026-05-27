package com.complipilot.backend.evidence.dto;

import java.time.Instant;
import java.util.UUID;

public record ComplianceItemEvidenceResponse(
        UUID linkId,
        UUID complianceItemId,
        EvidenceDocumentResponse evidence,
        UUID linkedByUserId,
        Instant linkedAt
) {
}