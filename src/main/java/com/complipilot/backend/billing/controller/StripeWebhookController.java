package com.complipilot.backend.billing.controller;

import com.complipilot.backend.billing.config.StripeProperties;
import com.complipilot.backend.billing.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeProperties stripeProperties;
    private final StripeWebhookService stripeWebhookService;

    public StripeWebhookController(StripeProperties stripeProperties, StripeWebhookService stripeWebhookService) {
        this.stripeProperties = stripeProperties;
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        String endpointSecret = stripeProperties.getWebhookSecret();

        if (endpointSecret == null || endpointSecret.isBlank()) {
            log.warn("Stripe webhook secret is not configured, returning 200 OK without processing");
            return ResponseEntity.ok().build();
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Failed to parse Stripe webhook event", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        log.info("Received Stripe webhook event: {}", event.getType());

        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    Session session = null;
                    if (event.getDataObjectDeserializer().getObject().isPresent()) {
                        session = (Session) event.getDataObjectDeserializer().getObject().get();
                    } else {
                        log.warn("Stripe API version mismatch. Attempting unsafe deserialization.");
                        session = (Session) event.getDataObjectDeserializer().deserializeUnsafe();
                    }

                    if (session != null) {
                        stripeWebhookService.handleCheckoutSessionCompleted(session);
                    } else {
                        log.error("Failed to deserialize Stripe Session from webhook event");
                    }
                    break;
                // You can handle other events like invoice.payment_succeeded here
                default:
                    log.debug("Unhandled event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error processing Stripe event", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event");
        }

        return ResponseEntity.ok().build();
    }
}
