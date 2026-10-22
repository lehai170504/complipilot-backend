package com.complipilot.backend.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.complipilot.backend.notification.enums.NotificationType;
import org.junit.jupiter.api.Test;

class NotificationTypeTest {

    @Test
    void shouldReadPersistedBillingPlanChangeApprovalType() {
        assertEquals(
                NotificationType.BILLING_PLAN_CHANGE_APPROVED,
                NotificationType.valueOf("BILLING_PLAN_CHANGE_APPROVED")
        );
    }
}
