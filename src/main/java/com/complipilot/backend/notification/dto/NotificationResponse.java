package com.complipilot.backend.notification.dto;

import java.time.Instant;
import java.util.UUID;

import com.complipilot.backend.notification.enums.NotificationType;

public record NotificationResponse(
        UUID id,
        UUID organizationId,
        UUID recipientUserId,
        NotificationType type,
        String title,
        String message,
        String resourceType,
        UUID resourceId,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}