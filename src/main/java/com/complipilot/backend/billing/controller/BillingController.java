package com.complipilot.backend.billing.controller;

import java.util.UUID;

import com.complipilot.backend.billing.dto.CheckoutSessionResponse;
import com.complipilot.backend.billing.dto.CustomerPortalResponse;
import com.complipilot.backend.billing.dto.CreateCheckoutSessionRequest;
import com.complipilot.backend.billing.dto.OrganizationUsageResponse;
import com.complipilot.backend.billing.service.BillingCheckoutService;
import com.complipilot.backend.billing.service.UsageQuotaService;
import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.organization.service.TenantAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Billing", description = "Subscription plan and usage quota APIs")
@RestController
public class BillingController {

        private final UsageQuotaService usageQuotaService;
        private final TenantAccessService tenantAccessService;
        private final BillingCheckoutService billingCheckoutService;

        public BillingController(
                        UsageQuotaService usageQuotaService,
                        TenantAccessService tenantAccessService,
                        BillingCheckoutService billingCheckoutService) {
                this.usageQuotaService = usageQuotaService;
                this.tenantAccessService = tenantAccessService;
                this.billingCheckoutService = billingCheckoutService;
        }

        @Operation(summary = "Get organization usage and plan limits", description = "Returns the current subscription plan and usage counters for an organization.", security = @SecurityRequirement(name = "bearerAuth"))
        @GetMapping("/api/v1/organizations/{organizationId}/billing/usage")
        public ResponseEntity<OrganizationUsageResponse> getOrganizationUsage(
                        @PathVariable UUID organizationId,
                        @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
                tenantAccessService.requireActiveMember(
                                organizationId,
                                authenticatedUser.id());

                return ResponseEntity.ok(
                                usageQuotaService.getOrganizationUsage(organizationId));
        }

        @Operation(summary = "Create billing checkout session", description = "Creates a checkout session for a plan upgrade. Currently returns a manual checkout placeholder.", security = @SecurityRequirement(name = "bearerAuth"))
        @PostMapping("/api/v1/organizations/{organizationId}/billing/checkout-session")
        public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
                        @PathVariable UUID organizationId,
                        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                        @Valid @RequestBody CreateCheckoutSessionRequest request) {
                return ResponseEntity.ok(
                                billingCheckoutService.createCheckoutSession(
                                                organizationId,
                                                authenticatedUser.id(),
                                                request));
        }

        @Operation(summary = "Create customer portal session", description = "Creates a session for the Stripe Customer Portal to manage billing history and payment methods.", security = @SecurityRequirement(name = "bearerAuth"))
        @PostMapping("/api/v1/organizations/{organizationId}/billing/customer-portal")
        public ResponseEntity<CustomerPortalResponse> createCustomerPortalSession(
                        @PathVariable UUID organizationId,
                        @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
                return ResponseEntity.ok(
                                billingCheckoutService.createCustomerPortalSession(
                                                organizationId,
                                                authenticatedUser.id()));
        }
}