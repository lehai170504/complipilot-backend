package com.complipilot.backend.evidence.dto;


import com.complipilot.backend.evidence.enums.EvidenceSourceType;
import com.complipilot.backend.evidence.enums.EvidenceType;

public record EvidenceFilterRequest(
        EvidenceType evidenceType,
        EvidenceSourceType sourceType
) {
}
