package com.complipilot.backend.billing.dto;

import com.complipilot.backend.billing.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;

public record CreateCheckoutSessionRequest(
        @NotNull
        SubscriptionPlan plan
) {
}