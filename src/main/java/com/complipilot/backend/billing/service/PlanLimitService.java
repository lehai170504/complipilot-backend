package com.complipilot.backend.billing.service;

import com.complipilot.backend.billing.enums.SubscriptionPlan;
import org.springframework.stereotype.Service;

@Service
public class PlanLimitService {

    private static final long MB = 1024L * 1024L;
    private static final long GB = 1024L * MB;

    public PlanLimit getLimits(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> new PlanLimit(
                    3,
                    50,
                    100 * MB,
                    20
            );
            case PRO -> new PlanLimit(
                    20,
                    1_000,
                    2 * GB,
                    500
            );
            case BUSINESS -> new PlanLimit(
                    50,
                    10_000,
                    20 * GB,
                    5_000
            );
            case ENTERPRISE -> new PlanLimit(
                    500,
                    100_000,
                    500 * GB,
                    100_000
            );
        };
    }
}
