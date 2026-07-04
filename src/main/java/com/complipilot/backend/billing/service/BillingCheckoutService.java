package com.complipilot.backend.billing.service;

import java.util.UUID;

import com.complipilot.backend.billing.dto.CheckoutSessionResponse;
import com.complipilot.backend.billing.dto.CreateCheckoutSessionRequest;
import com.complipilot.backend.billing.entity.OrganizationSubscription;
import com.complipilot.backend.billing.repository.OrganizationSubscriptionRepository;
import com.complipilot.backend.common.error.ConflictException;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.organization.service.TenantAccessService;
import com.complipilot.backend.billing.config.StripeProperties;
import com.complipilot.backend.billing.enums.SubscriptionPlan;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(BillingCheckoutService.class);

    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final TenantAccessService tenantAccessService;
    private final StripeProperties stripeProperties;
    private final String frontendBaseUrl;

    public BillingCheckoutService(
            OrganizationSubscriptionRepository subscriptionRepository,
            TenantAccessService tenantAccessService,
            StripeProperties stripeProperties,
            @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.subscriptionRepository = subscriptionRepository;
        this.tenantAccessService = tenantAccessService;
        this.stripeProperties = stripeProperties;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional(readOnly = true)
    public CheckoutSessionResponse createCheckoutSession(
            UUID organizationId,
            UUID currentUserId,
            CreateCheckoutSessionRequest request) {
        tenantAccessService.requireAdminRole(organizationId, currentUserId);

        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganization_Id(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization subscription not found"));

        if (subscription.getPlan() == request.plan()) {
            throw new ConflictException("Organization is already on this plan");
        }

        String priceId = getPriceIdForPlan(request.plan());

        if (priceId == null || priceId.isBlank() || stripeProperties.getApiKey() == null || stripeProperties.getApiKey().isBlank()) {
            // Fallback for when Stripe is not configured
            log.warn("Stripe price ID or API key is not configured. Falling back to manual mode.");
            return new CheckoutSessionResponse(
                    "MANUAL",
                    request.plan(),
                    null,
                    "Online checkout is not connected yet. Please submit a plan change request for platform admin review.");
        }

        try {
            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(frontendBaseUrl + "/dashboard")
                    .setCancelUrl(frontendBaseUrl + "/dashboard")
                    .setClientReferenceId(organizationId.toString())
                    .putMetadata("plan", request.plan().name())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build());

            if (subscription.getStripeCustomerId() != null && !subscription.getStripeCustomerId().isBlank()) {
                builder.setCustomer(subscription.getStripeCustomerId());
            }

            Session session = Session.create(builder.build());

            return new CheckoutSessionResponse(
                    "STRIPE",
                    request.plan(),
                    session.getUrl(),
                    null);

        } catch (StripeException e) {
            log.error("Failed to create Stripe checkout session", e);
            throw new ConflictException("Failed to initiate payment. Please try again later.");
        }
    }

    @Transactional(readOnly = true)
    public com.complipilot.backend.billing.dto.CustomerPortalResponse createCustomerPortalSession(
            UUID organizationId,
            UUID currentUserId) {
        tenantAccessService.requireAdminRole(organizationId, currentUserId);

        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganization_Id(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization subscription not found"));

        if (subscription.getStripeCustomerId() == null || subscription.getStripeCustomerId().isBlank()) {
            throw new ConflictException("No active billing account found. Please upgrade to a paid plan first.");
        }

        try {
            com.stripe.param.billingportal.SessionCreateParams params = new com.stripe.param.billingportal.SessionCreateParams.Builder()
                    .setCustomer(subscription.getStripeCustomerId())
                    .setReturnUrl(frontendBaseUrl + "/dashboard")
                    .build();

            com.stripe.model.billingportal.Session session = com.stripe.model.billingportal.Session.create(params);

            return new com.complipilot.backend.billing.dto.CustomerPortalResponse(session.getUrl());
        } catch (StripeException e) {
            log.error("Failed to create Stripe customer portal session", e);
            throw new ConflictException("Failed to access billing portal. Please try again later.");
        }
    }

    private String getPriceIdForPlan(SubscriptionPlan plan) {
        return switch (plan) {
            case PRO -> stripeProperties.getProPriceId();
            case BUSINESS -> stripeProperties.getBusinessPriceId();
            case ENTERPRISE -> stripeProperties.getEnterprisePriceId();
            default -> null;
        };
    }
}