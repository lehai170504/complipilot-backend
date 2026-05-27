package com.complipilot.backend.compliance.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.complipilot.backend.compliance.CompanyComplianceStatusWorkflow;
import com.complipilot.backend.compliance.enums.CompanyComplianceStatus;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "company_compliance_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_company_compliance_items_org_requirement",
                        columnNames = {"organization_id", "requirement_id"}
                )
        }
)
public class CompanyComplianceItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id", nullable = false)
    private ComplianceRequirement requirement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CompanyComplianceStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User ownerUser;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CompanyComplianceItem() {
    }

    public CompanyComplianceItem(
            Organization organization,
            ComplianceRequirement requirement
    ) {
        this.id = UUID.randomUUID();
        this.organization = organization;
        this.requirement = requirement;
        this.status = CompanyComplianceStatus.OPEN;
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

    public void updateStatus(CompanyComplianceStatus status) {
        CompanyComplianceStatusWorkflow.validateTransition(this.status, status);
        this.status = status;
    }

    public void updateDetails(User ownerUser, LocalDate dueDate, String notes) {
        this.ownerUser = ownerUser;
        this.dueDate = dueDate;
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public ComplianceRequirement getRequirement() {
        return requirement;
    }

    public CompanyComplianceStatus getStatus() {
        return status;
    }

    public User getOwnerUser() {
        return ownerUser;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getNotes() {
        return notes;
    }
}