package com.complipilot.backend.billing.dto;

import java.util.UUID;

import com.complipilot.backend.billing.enums.SubscriptionPlan;
import com.complipilot.backend.billing.enums.SubscriptionStatus;

public record OrganizationUsageResponse(
        UUID organizationId,
        SubscriptionPlan plan,
        SubscriptionStatus subscriptionStatus,

        long memberCount,
        long memberLimit,

        long evidenceDocumentCount,
        long evidenceDocumentLimit,

        long storageUsedBytes,
        long storageLimitBytes,

        long aiAnalysisCountThisMonth,
        long aiAnalysisLimitPerMonth
) {
}