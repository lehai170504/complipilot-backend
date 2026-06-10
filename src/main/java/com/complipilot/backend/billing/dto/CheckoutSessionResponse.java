package com.complipilot.backend.billing.dto;

import com.complipilot.backend.billing.enums.SubscriptionPlan;

public record CheckoutSessionResponse(
        String provider,
        SubscriptionPlan plan,
        String checkoutUrl,
        String message
) {
}