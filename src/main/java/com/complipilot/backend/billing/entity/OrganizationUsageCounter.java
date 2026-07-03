package com.complipilot.backend.billing.entity;

import java.time.Instant;
import java.util.UUID;

import com.complipilot.backend.organization.entity.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "organization_usage_counters")
public class OrganizationUsageCounter {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "period_month", nullable = false, length = 7)
    private String periodMonth;

    @Column(name = "evidence_document_count", nullable = false)
    private long evidenceDocumentCount;

    @Column(name = "storage_bytes", nullable = false)
    private long storageBytes;

    @Column(name = "ai_analysis_count", nullable = false)
    private long aiAnalysisCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected OrganizationUsageCounter() {
    }

    public OrganizationUsageCounter(Organization organization, String periodMonth) {
        this.id = UUID.randomUUID();
        this.organization = organization;
        this.periodMonth = periodMonth;
        this.evidenceDocumentCount = 0;
        this.storageBytes = 0;
        this.aiAnalysisCount = 0;
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

    public void recordEvidenceCreated(long fileSizeBytes) {
        this.evidenceDocumentCount++;
        this.storageBytes += Math.max(fileSizeBytes, 0);
    }

    public void recordAiAnalysisRun() {
        this.aiAnalysisCount++;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getPeriodMonth() {
        return periodMonth;
    }

    public long getEvidenceDocumentCount() {
        return evidenceDocumentCount;
    }

    public long getStorageBytes() {
        return storageBytes;
    }

    public long getAiAnalysisCount() {
        return aiAnalysisCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
