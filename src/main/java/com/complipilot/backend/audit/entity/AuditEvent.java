package com.complipilot.backend.audit.entity;

import com.complipilot.backend.audit.enums.AuditAction;
import com.complipilot.backend.audit.enums.AuditResourceType;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.organization.entity.Organization;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 100)
    private AuditResourceType resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(
            Organization organization,
            User actorUser,
            AuditAction action,
            AuditResourceType resourceType,
            UUID resourceId,
            String summary,
            String metadataJson
    ) {
        this.id = UUID.randomUUID();
        this.organization = organization;
        this.actorUser = actorUser;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.summary = summary;
        this.metadataJson = metadataJson;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }

        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public User getActorUser() {
        return actorUser;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditResourceType getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getSummary() {
        return summary;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
