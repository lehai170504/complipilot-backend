package com.complipilot.backend.organization.entity;

import java.time.Instant;
import java.util.UUID;

import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.organization.enums.OrganizationInvitationStatus;
import com.complipilot.backend.organization.enums.OrganizationMemberRole;

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
@Table(name = "organization_invitations")
public class OrganizationInvitation {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrganizationMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrganizationInvitationStatus status;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private User invitedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_user_id")
    private User acceptedByUser;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationInvitation() {
    }

    public OrganizationInvitation(
            Organization organization,
            String email,
            OrganizationMemberRole role,
            String tokenHash,
            User invitedByUser,
            Instant expiresAt
    ) {
        this.id = UUID.randomUUID();
        this.organization = organization;
        this.email = email.toLowerCase().trim();
        this.role = role;
        this.status = OrganizationInvitationStatus.PENDING;
        this.tokenHash = tokenHash;
        this.invitedByUser = invitedByUser;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();

        if (this.id == null) {
            this.id = UUID.randomUUID();
        }

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void accept(User acceptedByUser) {
        this.status = OrganizationInvitationStatus.ACCEPTED;
        this.acceptedByUser = acceptedByUser;
        this.acceptedAt = Instant.now();
    }

    public void revoke() {
        this.status = OrganizationInvitationStatus.REVOKED;
    }

    public void expire() {
        this.status = OrganizationInvitationStatus.EXPIRED;
    }

    public boolean isPending() {
        return this.status == OrganizationInvitationStatus.PENDING;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getEmail() {
        return email;
    }

    public OrganizationMemberRole getRole() {
        return role;
    }

    public OrganizationInvitationStatus getStatus() {
        return status;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public User getInvitedByUser() {
        return invitedByUser;
    }

    public User getAcceptedByUser() {
        return acceptedByUser;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
