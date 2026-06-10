package com.complipilot.backend.billing.service;

import java.util.UUID;

import com.complipilot.backend.billing.dto.CheckoutSessionResponse;
import com.complipilot.backend.billing.dto.CreateCheckoutSessionRequest;
import com.complipilot.backend.billing.entity.OrganizationSubscription;
import com.complipilot.backend.billing.repository.OrganizationSubscriptionRepository;
import com.complipilot.backend.common.error.ConflictException;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.organization.service.TenantAccessService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingCheckoutService {

    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final TenantAccessService tenantAccessService;

    public BillingCheckoutService(
            OrganizationSubscriptionRepository subscriptionRepository,
            TenantAccessService tenantAccessService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.tenantAccessService = tenantAccessService;
    }

    @Transactional(readOnly = true)
    public CheckoutSessionResponse createCheckoutSession(
            UUID organizationId,
            UUID currentUserId,
            CreateCheckoutSessionRequest request
    ) {
        tenantAccessService.requireAdminRole(organizationId, currentUserId);

        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganization_Id(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization subscription not found"));

        if (subscription.getPlan() == request.plan()) {
            throw new ConflictException("Organization is already on this plan");
        }

        return new CheckoutSessionResponse(
                "MANUAL",
                request.plan(),
                null,
                "Online checkout is not connected yet. Please submit a plan change request for platform admin review."
        );
    }
}