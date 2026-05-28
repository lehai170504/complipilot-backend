package com.complipilot.backend.audit.dto;

import com.complipilot.backend.audit.enums.AuditAction;
import com.complipilot.backend.audit.enums.AuditResourceType;

public record AuditEventFilterRequest(
        AuditAction action,
        AuditResourceType resourceType
) {
}
