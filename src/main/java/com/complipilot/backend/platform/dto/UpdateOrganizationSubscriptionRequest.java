package com.complipilot.backend.platform.dto;

import com.complipilot.backend.billing.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationSubscriptionRequest(
        @NotNull
        SubscriptionPlan plan
) {
}