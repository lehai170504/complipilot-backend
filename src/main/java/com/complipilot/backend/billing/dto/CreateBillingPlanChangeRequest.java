package com.complipilot.backend.billing.dto;

import com.complipilot.backend.billing.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBillingPlanChangeRequest(
        @NotNull
        SubscriptionPlan requestedPlan,

        @Size(max = 1000)
        String requestNote
) {
}