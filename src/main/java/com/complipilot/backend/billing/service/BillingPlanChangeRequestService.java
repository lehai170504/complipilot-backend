package com.complipilot.backend.billing.service;

import java.util.UUID;

import com.complipilot.backend.billing.dto.BillingPlanChangeRequestResponse;
import com.complipilot.backend.billing.dto.CreateBillingPlanChangeRequest;
import com.complipilot.backend.billing.entity.BillingPlanChangeRequest;
import com.complipilot.backend.billing.entity.OrganizationSubscription;
import com.complipilot.backend.billing.enums.BillingPlanChangeRequestStatus;
import com.complipilot.backend.billing.repository.BillingPlanChangeRequestRepository;
import com.complipilot.backend.billing.repository.OrganizationSubscriptionRepository;
import com.complipilot.backend.common.error.ConflictException;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.common.pagination.PageResponse;
import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.common.security.PlatformAdminService;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;
import com.complipilot.backend.notification.enums.NotificationType;
import com.complipilot.backend.notification.service.NotificationService;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.repository.OrganizationRepository;
import com.complipilot.backend.organization.service.TenantAccessService;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingPlanChangeRequestService {

    private final BillingPlanChangeRequestRepository requestRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TenantAccessService tenantAccessService;
    private final PlatformAdminService platformAdminService;
    private final NotificationService notificationService;

    public BillingPlanChangeRequestService(
            BillingPlanChangeRequestRepository requestRepository,
            OrganizationSubscriptionRepository subscriptionRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            TenantAccessService tenantAccessService,
            PlatformAdminService platformAdminService,
            NotificationService notificationService
    ) {
        this.requestRepository = requestRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.tenantAccessService = tenantAccessService;
        this.platformAdminService = platformAdminService;
        this.notificationService = notificationService;
    }

    @Transactional
    public BillingPlanChangeRequestResponse createRequest(
            UUID organizationId,
            UUID currentUserId,
            CreateBillingPlanChangeRequest request
    ) {
        tenantAccessService.requireAdminRole(organizationId, currentUserId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        User requestedByUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganization_Id(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization subscription not found"));

        if (subscription.getPlan() == request.requestedPlan()) {
            throw new ConflictException("Organization is already on this plan");
        }

        boolean hasPendingRequest = requestRepository.existsByOrganization_IdAndStatus(
                organizationId,
                BillingPlanChangeRequestStatus.PENDING
        );

        if (hasPendingRequest) {
            throw new ConflictException("There is already a pending plan change request");
        }

        BillingPlanChangeRequest entity = requestRepository.save(
                new BillingPlanChangeRequest(
                        organization,
                        requestedByUser,
                        subscription.getPlan(),
                        request.requestedPlan()
                )
        );

        notificationService.notifyUser(
                organizationId,
                currentUserId,
                NotificationType.BILLING_PLAN_CHANGE_REQUESTED,
                "Plan change request submitted",
                "Your request to change from %s to %s has been submitted for platform admin review."
                        .formatted(subscription.getPlan(), request.requestedPlan()),
                "BILLING_PLAN_CHANGE_REQUEST",
                entity.getId()
        );

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public BillingPlanChangeRequestResponse getLatestRequest(
            UUID organizationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        return requestRepository
                .findTopByOrganization_IdOrderByCreatedAtDesc(organizationId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResponse<BillingPlanChangeRequestResponse> listPlatformRequests(
            AuthenticatedUser authenticatedUser,
            BillingPlanChangeRequestStatus status,
            int page,
            int size
    ) {
        platformAdminService.requirePlatformAdmin(authenticatedUser);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize);

        if (status == null) {
            return PageResponse.from(
                    requestRepository
                            .findAllByOrderByCreatedAtDesc(pageable)
                            .map(this::toResponse)
            );
        }

        return PageResponse.from(
                requestRepository
                        .findByStatusOrderByCreatedAtDesc(status, pageable)
                        .map(this::toResponse)
        );
    }

    @Transactional
    public BillingPlanChangeRequestResponse approveRequest(
            AuthenticatedUser authenticatedUser,
            UUID requestId
    ) {
        platformAdminService.requirePlatformAdmin(authenticatedUser);

        BillingPlanChangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Plan change request not found"));

        if (!request.isPending()) {
            throw new ConflictException("Only pending requests can be approved");
        }

        User reviewedByUser = userRepository.findById(authenticatedUser.id())
                .orElseThrow(() -> new NotFoundException("Reviewer user not found"));

        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganization_Id(request.getOrganization().getId())
                .orElseThrow(() -> new NotFoundException("Organization subscription not found"));

        subscription.changePlan(request.getRequestedPlan());
        request.approve(reviewedByUser);

        notificationService.notifyUser(
                request.getOrganization().getId(),
                request.getRequestedByUser().getId(),
                NotificationType.BILLING_PLAN_CHANGE_APPROVED,
                "Plan change approved",
                "Your request to change from %s to %s has been approved."
                        .formatted(request.getCurrentPlan(), request.getRequestedPlan()),
                "BILLING_PLAN_CHANGE_REQUEST",
                request.getId()
        );

        return toResponse(request);
    }

    @Transactional
    public BillingPlanChangeRequestResponse rejectRequest(
            AuthenticatedUser authenticatedUser,
            UUID requestId
    ) {
        platformAdminService.requirePlatformAdmin(authenticatedUser);

        BillingPlanChangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Plan change request not found"));

        if (!request.isPending()) {
            throw new ConflictException("Only pending requests can be rejected");
        }

        User reviewedByUser = userRepository.findById(authenticatedUser.id())
                .orElseThrow(() -> new NotFoundException("Reviewer user not found"));

        request.reject(reviewedByUser);

        notificationService.notifyUser(
                request.getOrganization().getId(),
                request.getRequestedByUser().getId(),
                NotificationType.BILLING_PLAN_CHANGE_REJECTED,
                "Plan change rejected",
                "Your request to change from %s to %s was rejected."
                        .formatted(request.getCurrentPlan(), request.getRequestedPlan()),
                "BILLING_PLAN_CHANGE_REQUEST",
                request.getId()
        );

        return toResponse(request);
    }

    private BillingPlanChangeRequestResponse toResponse(
            BillingPlanChangeRequest request
    ) {
        User reviewedByUser = request.getReviewedByUser();

        return new BillingPlanChangeRequestResponse(
                request.getId(),
                request.getOrganization().getId(),
                request.getOrganization().getName(),
                request.getRequestedByUser().getId(),
                request.getRequestedByUser().getEmail(),
                request.getCurrentPlan(),
                request.getRequestedPlan(),
                request.getStatus(),
                reviewedByUser == null ? null : reviewedByUser.getId(),
                reviewedByUser == null ? null : reviewedByUser.getEmail(),
                request.getReviewedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}