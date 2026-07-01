package com.complipilot.backend.billing.service;

import java.util.UUID;

import com.complipilot.backend.billing.entity.OrganizationSubscription;
import com.complipilot.backend.billing.enums.SubscriptionPlan;
import com.complipilot.backend.billing.repository.OrganizationSubscriptionRepository;
import com.stripe.model.checkout.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final OrganizationSubscriptionRepository subscriptionRepository;

    public StripeWebhookService(OrganizationSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public void handleCheckoutSessionCompleted(Session session) {
        String clientReferenceId = session.getClientReferenceId();

        if (clientReferenceId == null || clientReferenceId.isBlank()) {
            log.warn("Ignoring checkout session without clientReferenceId: {}", session.getId());
            return;
        }

        UUID organizationId;
        try {
            organizationId = UUID.fromString(clientReferenceId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid clientReferenceId format (not a UUID): {}", clientReferenceId);
            return;
        }

        String planString = session.getMetadata() != null ? session.getMetadata().get("plan") : null;
        if (planString == null) {
            log.warn("No 'plan' metadata found on checkout session: {}", session.getId());
            return;
        }

        SubscriptionPlan newPlan;
        try {
            newPlan = SubscriptionPlan.valueOf(planString);
        } catch (IllegalArgumentException e) {
            log.error("Unknown plan in metadata: {}", planString);
            return;
        }

        OrganizationSubscription orgSub = subscriptionRepository.findByOrganization_Id(organizationId)
                .orElse(null);

        if (orgSub == null) {
            log.error("OrganizationSubscription not found for organizationId: {}", organizationId);
            return;
        }

        orgSub.changePlan(newPlan);
        orgSub.setStripeCustomerId(session.getCustomer());
        orgSub.setStripeSubscriptionId(session.getSubscription());
        orgSub.setCancelAtPeriodEnd(false);

        subscriptionRepository.save(orgSub);

        log.info("Successfully updated organization {} to plan {}", organizationId, newPlan);
    }
}
