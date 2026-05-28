package com.complipilot.backend.audit.dto;

import com.complipilot.backend.audit.enums.AuditAction;
import com.complipilot.backend.audit.enums.AuditResourceType;

import java.time.Instant;
import java.util.UUID;



public record AuditEventResponse(
        UUID id,
        UUID organizationId,
        UUID actorUserId,
        String actorEmail,
        AuditAction action,
        AuditResourceType resourceType,
        UUID resourceId,
        String summary,
        String metadataJson,
        Instant createdAt
) {
}