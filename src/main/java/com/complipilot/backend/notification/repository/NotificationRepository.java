package com.complipilot.backend.notification.repository;

import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByOrganization_IdAndRecipientUser_IdOrderByCreatedAtDesc(
            UUID organizationId,
            UUID recipientUserId,
            Pageable pageable
    );

    Optional<Notification> findByIdAndOrganization_IdAndRecipientUser_Id(
            UUID id,
            UUID organizationId,
            UUID recipientUserId
    );

    long countByOrganization_IdAndRecipientUser_IdAndReadAtIsNull(
            UUID organizationId,
            UUID recipientUserId
    );
}