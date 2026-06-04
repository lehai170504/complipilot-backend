package com.complipilot.backend.platform.service;

import java.time.YearMonth;
import java.util.UUID;

import com.complipilot.backend.billing.dto.OrganizationUsageResponse;
import com.complipilot.backend.billing.entity.OrganizationSubscription;
import com.complipilot.backend.billing.entity.OrganizationUsageCounter;
import com.complipilot.backend.billing.enums.SubscriptionPlan;
import com.complipilot.backend.billing.enums.SubscriptionStatus;
import com.complipilot.backend.billing.repository.OrganizationSubscriptionRepository;
import com.complipilot.backend.billing.repository.OrganizationUsageCounterRepository;
import com.complipilot.backend.billing.service.UsageQuotaService;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.common.pagination.PageResponse;
import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.common.security.PlatformAdminService;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.enums.OrganizationMemberStatus;
import com.complipilot.backend.organization.repository.OrganizationMemberRepository;
import com.complipilot.backend.organization.repository.OrganizationRepository;
import com.complipilot.backend.platform.dto.PlatformOrganizationResponse;
import com.complipilot.backend.platform.dto.PlatformUserResponse;

import com.complipilot.backend.platform.dto.UpdateOrganizationSubscriptionRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformAdminApiService {

    private final PlatformAdminService platformAdminService;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final OrganizationUsageCounterRepository usageCounterRepository;
    private final UsageQuotaService usageQuotaService;

    public PlatformAdminApiService(
            PlatformAdminService platformAdminService,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            OrganizationMemberRepository organizationMemberRepository,
            OrganizationSubscriptionRepository subscriptionRepository,
            OrganizationUsageCounterRepository usageCounterRepository,
            UsageQuotaService usageQuotaService
    ) {
        this.platformAdminService = platformAdminService;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.usageCounterRepository = usageCounterRepository;
        this.usageQuotaService = usageQuotaService;
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformOrganizationResponse> listOrganizations(
            AuthenticatedUser authenticatedUser,
            int page,
            int size
    ) {
        platformAdminService.requirePlatformAdmin(authenticatedUser);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        return PageResponse.from(
                organizationRepository
                        .findAll(
                                PageRequest.of(
                                        safePage,
                                        safeSize,
                                        Sort.by(Sort.Direction.DESC, "createdAt")
                                )
                        )
                        .map(this::toOrganizationResponse)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformUserResponse> listUsers(
            AuthenticatedUser authenticatedUser,
            int page,
            int size
    ) {
        platformAdminService.requirePlatformAdmin(authenticatedUser);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        return PageResponse.from(
                userRepository
                        .findAll(
                                PageRequest.of(
                                        safePage,
                                        safeSize,
                                        Sort.by(Sort.Direction.DESC, "createdAt")
                                )
                        )
                        .map(this::toUserResponse)
        );
    }

    @Transactional(readOnly = true)
    public OrganizationUsageResponse getOrganizationUsage(
            AuthenticatedUser authenticatedUser,
            UUID organizationId
    ) {
        platformAdminService.requirePlatformAdmin(authenticatedUser);

        return usageQuotaService.getOrganizationUsage(organizationId);
    }

    @Transactional
    public OrganizationUsageResponse updateOrganizationSubscription(
            AuthenticatedUser authenticatedUser,
            UUID organizationId,
            UpdateOrganizationSubscriptionRequest request
    ) {
        platformAdminService.requirePlatformAdmin(authenticatedUser);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganization_Id(organizationId)
                .orElseGet(() -> subscriptionRepository.save(
                        new OrganizationSubscription(
                                organization,
                                request.plan()
                        )
                ));

        subscription.changePlan(request.plan());

        return usageQuotaService.getOrganizationUsage(organizationId);
    }

    private PlatformOrganizationResponse toOrganizationResponse(
            Organization organization
    ) {
        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganization_Id(organization.getId())
                .orElse(null);

        OrganizationUsageCounter usageCounter = usageCounterRepository
                .findByOrganization_IdAndPeriodMonth(
                        organization.getId(),
                        currentPeriodMonth()
                )
                .orElse(null);

        long activeMemberCount = organizationMemberRepository
                .countByOrganization_IdAndStatus(
                        organization.getId(),
                        OrganizationMemberStatus.ACTIVE
                );

        SubscriptionPlan plan = subscription == null
                ? null
                : subscription.getPlan();

        SubscriptionStatus subscriptionStatus = subscription == null
                ? null
                : subscription.getStatus();

        long evidenceDocumentCount = usageCounter == null
                ? 0L
                : usageCounter.getEvidenceDocumentCount();

        long storageBytes = usageCounter == null
                ? 0L
                : usageCounter.getStorageBytes();

        long aiAnalysisCount = usageCounter == null
                ? 0L
                : usageCounter.getAiAnalysisCount();

        return new PlatformOrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getStatus(),
                plan,
                subscriptionStatus,
                activeMemberCount,
                evidenceDocumentCount,
                storageBytes,
                aiAnalysisCount,
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
    }

    private PlatformUserResponse toUserResponse(User user) {
        return new PlatformUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String currentPeriodMonth() {
        return YearMonth.now().toString();
    }
}