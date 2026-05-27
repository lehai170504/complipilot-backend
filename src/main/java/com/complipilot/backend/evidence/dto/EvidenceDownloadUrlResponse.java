package com.complipilot.backend.evidence.dto;

public record EvidenceDownloadUrlResponse(
        String downloadUrl,
        String method,
        int expiresInMinutes
) {
}