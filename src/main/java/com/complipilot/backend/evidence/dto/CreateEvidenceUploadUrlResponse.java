package com.complipilot.backend.evidence.dto;

public record CreateEvidenceUploadUrlResponse(
        String objectKey,
        String uploadUrl,
        String method,
        int expiresInMinutes,
        String uploadToken
) {
}