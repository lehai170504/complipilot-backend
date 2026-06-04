package com.complipilot.backend.notification.service;

import java.util.UUID;

import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.common.pagination.PageResponse;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;
import com.complipilot.backend.notification.dto.NotificationResponse;
import com.complipilot.backend.notification.entity.Notification;
import com.complipilot.backend.notification.enums.NotificationType;
import com.complipilot.backend.notification.repository.NotificationRepository;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.repository.OrganizationRepository;
import com.complipilot.backend.organization.service.TenantAccessService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TenantAccessService tenantAccessService;

    public NotificationService(
            NotificationRepository notificationRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            TenantAccessService tenantAccessService
    ) {
        this.notificationRepository = notificationRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.tenantAccessService = tenantAccessService;
    }

    @Transactional
    public void notifyUser(
            UUID organizationId,
            UUID recipientUserId,
            NotificationType type,
            String title,
            String message,
            String resourceType,
            UUID resourceId
    ) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        User recipientUser = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new NotFoundException("Recipient user not found"));

        notificationRepository.save(
                new Notification(
                        organization,
                        recipientUser,
                        type,
                        title,
                        message,
                        resourceType,
                        resourceId
                )
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listNotifications(
            UUID organizationId,
            UUID currentUserId,
            int page,
            int size
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        return PageResponse.from(
                notificationRepository
                        .findByOrganization_IdAndRecipientUser_IdOrderByCreatedAtDesc(
                                organizationId,
                                currentUserId,
                                PageRequest.of(safePage, safeSize)
                        )
                        .map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public long countUnreadNotifications(
            UUID organizationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        return notificationRepository
                .countByOrganization_IdAndRecipientUser_IdAndReadAtIsNull(
                        organizationId,
                        currentUserId
                );
    }

    @Transactional
    public NotificationResponse markAsRead(
            UUID organizationId,
            UUID notificationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        Notification notification = notificationRepository
                .findByIdAndOrganization_IdAndRecipientUser_Id(
                        notificationId,
                        organizationId,
                        currentUserId
                )
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        notification.markAsRead();

        return toResponse(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getOrganization().getId(),
                notification.getRecipientUser().getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getResourceType(),
                notification.getResourceId(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}