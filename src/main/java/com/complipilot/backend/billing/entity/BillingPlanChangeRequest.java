package com.complipilot.backend.billing.entity;

import java.time.Instant;
import java.util.UUID;

import com.complipilot.backend.billing.enums.BillingPlanChangeRequestStatus;
import com.complipilot.backend.billing.enums.SubscriptionPlan;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.organization.entity.Organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "billing_plan_change_requests")
public class BillingPlanChangeRequest {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_plan", nullable = false, length = 50)
    private SubscriptionPlan currentPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_plan", nullable = false, length = 50)
    private SubscriptionPlan requestedPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BillingPlanChangeRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedByUser;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BillingPlanChangeRequest() {
    }

    public BillingPlanChangeRequest(
            Organization organization,
            User requestedByUser,
            SubscriptionPlan currentPlan,
            SubscriptionPlan requestedPlan
    ) {
        this.id = UUID.randomUUID();
        this.organization = organization;
        this.requestedByUser = requestedByUser;
        this.currentPlan = currentPlan;
        this.requestedPlan = requestedPlan;
        this.status = BillingPlanChangeRequestStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();

        if (this.id == null) {
            this.id = UUID.randomUUID();
        }

        if (this.status == null) {
            this.status = BillingPlanChangeRequestStatus.PENDING;
        }

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void approve(User reviewedByUser) {
        this.status = BillingPlanChangeRequestStatus.APPROVED;
        this.reviewedByUser = reviewedByUser;
        this.reviewedAt = Instant.now();
    }

    public void reject(User reviewedByUser) {
        this.status = BillingPlanChangeRequestStatus.REJECTED;
        this.reviewedByUser = reviewedByUser;
        this.reviewedAt = Instant.now();
    }

    public void cancel() {
        this.status = BillingPlanChangeRequestStatus.CANCELLED;
    }

    public boolean isPending() {
        return this.status == BillingPlanChangeRequestStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public User getRequestedByUser() {
        return requestedByUser;
    }

    public SubscriptionPlan getCurrentPlan() {
        return currentPlan;
    }

    public SubscriptionPlan getRequestedPlan() {
        return requestedPlan;
    }

    public BillingPlanChangeRequestStatus getStatus() {
        return status;
    }

    public User getReviewedByUser() {
        return reviewedByUser;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}