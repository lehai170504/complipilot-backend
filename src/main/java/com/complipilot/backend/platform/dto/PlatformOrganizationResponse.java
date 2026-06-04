package com.complipilot.backend.platform.dto;

import java.time.Instant;
import java.util.UUID;

import com.complipilot.backend.billing.enums.SubscriptionPlan;
import com.complipilot.backend.billing.enums.SubscriptionStatus;
import com.complipilot.backend.organization.enums.OrganizationStatus;

public record PlatformOrganizationResponse(
        UUID id,
        String name,
        String slug,
        OrganizationStatus status,

        SubscriptionPlan plan,
        SubscriptionStatus subscriptionStatus,

        long activeMemberCount,
        long evidenceDocumentCount,
        long storageBytes,
        long aiAnalysisCount,

        Instant createdAt,
        Instant updatedAt
) {
}