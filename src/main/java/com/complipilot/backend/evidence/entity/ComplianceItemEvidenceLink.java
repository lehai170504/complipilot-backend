package com.complipilot.backend.evidence.entity;

import java.time.Instant;
import java.util.UUID;

import com.complipilot.backend.compliance.entity.CompanyComplianceItem;
import com.complipilot.backend.identity.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "compliance_item_evidence_links",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_compliance_item_evidence_links_item_document",
                        columnNames = {"compliance_item_id", "evidence_document_id"}
                )
        }
)
public class ComplianceItemEvidenceLink {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compliance_item_id", nullable = false)
    private CompanyComplianceItem complianceItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evidence_document_id", nullable = false)
    private EvidenceDocument evidenceDocument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linked_by_user_id", nullable = false)
    private User linkedByUser;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    protected ComplianceItemEvidenceLink() {
    }

    public ComplianceItemEvidenceLink(
            CompanyComplianceItem complianceItem,
            EvidenceDocument evidenceDocument,
            User linkedByUser
    ) {
        this.id = UUID.randomUUID();
        this.complianceItem = complianceItem;
        this.evidenceDocument = evidenceDocument;
        this.linkedByUser = linkedByUser;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }

        if (this.linkedAt == null) {
            this.linkedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public CompanyComplianceItem getComplianceItem() {
        return complianceItem;
    }

    public EvidenceDocument getEvidenceDocument() {
        return evidenceDocument;
    }

    public User getLinkedByUser() {
        return linkedByUser;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }
}