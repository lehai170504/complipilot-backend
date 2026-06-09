package com.complipilot.backend.billing.dto;

import java.time.Instant;
import java.util.UUID;

import com.complipilot.backend.billing.enums.BillingPlanChangeRequestStatus;
import com.complipilot.backend.billing.enums.SubscriptionPlan;

public record BillingPlanChangeRequestResponse(
        UUID id,
        UUID organizationId,
        String organizationName,
        UUID requestedByUserId,
        String requestedByEmail,
        SubscriptionPlan currentPlan,
        SubscriptionPlan requestedPlan,
        BillingPlanChangeRequestStatus status,
        UUID reviewedByUserId,
        String reviewedByEmail,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
}