package com.complipilot.backend.billing.service;

import java.time.YearMonth;
import java.util.UUID;

import com.complipilot.backend.billing.dto.OrganizationUsageResponse;
import com.complipilot.backend.billing.entity.OrganizationSubscription;
import com.complipilot.backend.billing.entity.OrganizationUsageCounter;
import com.complipilot.backend.billing.enums.SubscriptionPlan;
import com.complipilot.backend.billing.repository.OrganizationSubscriptionRepository;
import com.complipilot.backend.billing.repository.OrganizationUsageCounterRepository;
import com.complipilot.backend.common.error.ConflictException;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.enums.OrganizationMemberStatus;
import com.complipilot.backend.organization.repository.OrganizationMemberRepository;
import com.complipilot.backend.organization.repository.OrganizationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageQuotaService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final OrganizationUsageCounterRepository usageCounterRepository;
    private final PlanLimitService planLimitService;

    public UsageQuotaService(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            OrganizationSubscriptionRepository subscriptionRepository,
            OrganizationUsageCounterRepository usageCounterRepository,
            PlanLimitService planLimitService
    ) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.usageCounterRepository = usageCounterRepository;
        this.planLimitService = planLimitService;
    }

    @Transactional
    public OrganizationSubscription createDefaultSubscription(Organization organization) {
        return subscriptionRepository.findByOrganization_Id(organization.getId())
                .orElseGet(() -> subscriptionRepository.save(
                        new OrganizationSubscription(
                                organization,
                                SubscriptionPlan.FREE
                        )
                ));
    }

    @Transactional(readOnly = true)
    public void requireCanUploadEvidenceFile(UUID organizationId, Long fileSizeBytes) {
        PlanLimit limits = getLimits(organizationId);
        OrganizationUsageCounter counter = getCurrentCounter(organizationId);
        long safeFileSizeBytes = safeFileSizeBytes(fileSizeBytes);

        if (counter.getEvidenceDocumentCount() >= limits.maxEvidenceDocuments()) {
            throw new ConflictException(
                    "Evidence document limit reached for the current plan"
            );
        }

        if (counter.getStorageBytes() + safeFileSizeBytes > limits.maxStorageBytes()) {
            throw new ConflictException(
                    "Storage limit reached for the current plan"
            );
        }
    }

    @Transactional(readOnly = true)
    public void requireCanCreateEvidence(UUID organizationId, Long fileSizeBytes) {
        requireCanUploadEvidenceFile(organizationId, fileSizeBytes);
    }

    @Transactional(readOnly = true)
    public void requireCanRunAiAnalysis(UUID organizationId) {
        PlanLimit limits = getLimits(organizationId);
        OrganizationUsageCounter counter = getCurrentCounter(organizationId);

        if (counter.getAiAnalysisCount() >= limits.maxAiAnalysesPerMonth()) {
            throw new ConflictException(
                    "AI analysis limit reached for the current billing period"
            );
        }
    }

    @Transactional(readOnly = true)
    public void requireCanAddMember(UUID organizationId) {
        PlanLimit limits = getLimits(organizationId);

        long activeMembers = organizationMemberRepository.countByOrganization_IdAndStatus(
                organizationId,
                OrganizationMemberStatus.ACTIVE
        );

        if (activeMembers >= limits.maxMembers()) {
            throw new ConflictException(
                    "Member limit reached for the current plan"
            );
        }
    }

    @Transactional
    public void recordEvidenceCreated(UUID organizationId, Long fileSizeBytes) {
        OrganizationUsageCounter counter = getOrCreateCurrentCounter(organizationId);
        counter.recordEvidenceCreated(safeFileSizeBytes(fileSizeBytes));
    }

    @Transactional
    public void recordAiAnalysisRun(UUID organizationId) {
        OrganizationUsageCounter counter = getOrCreateCurrentCounter(organizationId);
        counter.recordAiAnalysisRun();
    }

    @Transactional(readOnly = true)
    public OrganizationUsageResponse getOrganizationUsage(UUID organizationId) {
        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganization_Id(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization subscription not found"));

        if (!subscription.isActive()) {
            throw new ConflictException("Organization subscription is not active");
        }

        OrganizationUsageCounter counter = getCurrentCounter(organizationId);
        PlanLimit limits = planLimitService.getLimits(subscription.getPlan());

        long activeMembers = organizationMemberRepository.countByOrganization_IdAndStatus(
                organizationId,
                OrganizationMemberStatus.ACTIVE
        );

        return new OrganizationUsageResponse(
                organizationId,
                subscription.getPlan(),
                subscription.getStatus(),
                activeMembers,
                limits.maxMembers(),
                counter.getEvidenceDocumentCount(),
                limits.maxEvidenceDocuments(),
                counter.getStorageBytes(),
                limits.maxStorageBytes(),
                counter.getAiAnalysisCount(),
                limits.maxAiAnalysesPerMonth()
        );
    }

    private PlanLimit getLimits(UUID organizationId) {
        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganization_Id(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization subscription not found"));

        if (!subscription.isActive()) {
            throw new ConflictException("Organization subscription is not active");
        }

        return planLimitService.getLimits(subscription.getPlan());
    }

    private OrganizationUsageCounter getCurrentCounter(UUID organizationId) {
        return usageCounterRepository
                .findByOrganization_IdAndPeriodMonth(
                        organizationId,
                        currentPeriodMonth()
                )
                .orElseGet(() -> new OrganizationUsageCounter(
                        getOrganization(organizationId),
                        currentPeriodMonth()
                ));
    }

    private OrganizationUsageCounter getOrCreateCurrentCounter(UUID organizationId) {
        return usageCounterRepository
                .findByOrganization_IdAndPeriodMonth(
                        organizationId,
                        currentPeriodMonth()
                )
                .orElseGet(() -> usageCounterRepository.save(
                        new OrganizationUsageCounter(
                                getOrganization(organizationId),
                                currentPeriodMonth()
                        )
                ));
    }

    private Organization getOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private String currentPeriodMonth() {
        return YearMonth.now().toString();
    }

    private long safeFileSizeBytes(Long fileSizeBytes) {
        if (fileSizeBytes == null || fileSizeBytes < 0) {
            return 0L;
        }

        return fileSizeBytes;
    }
}