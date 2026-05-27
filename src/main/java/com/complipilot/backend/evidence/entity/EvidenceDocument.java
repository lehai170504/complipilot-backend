package com.complipilot.backend.evidence.entity;

import java.time.Instant;
import java.util.UUID;

import com.complipilot.backend.evidence.enums.EvidenceSourceType;
import com.complipilot.backend.evidence.enums.EvidenceStatus;
import com.complipilot.backend.evidence.enums.EvidenceType;
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
@Table(name = "evidence_documents")
public class EvidenceDocument {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 50)
    private EvidenceType evidenceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private EvidenceSourceType sourceType;

    @Column(name = "file_object_key", length = 500)
    private String fileObjectKey;

    @Column(name = "external_url", length = 1000)
    private String externalUrl;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedByUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EvidenceStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EvidenceDocument() {
    }

    public EvidenceDocument(
            Organization organization,
            String title,
            String description,
            EvidenceType evidenceType,
            EvidenceSourceType sourceType,
            String fileObjectKey,
            String externalUrl,
            String contentType,
            Long fileSizeBytes,
            User uploadedByUser
    ) {
        this.id = UUID.randomUUID();
        this.organization = organization;
        this.title = title.trim();
        this.description = description;
        this.evidenceType = evidenceType;
        this.sourceType = sourceType;
        this.fileObjectKey = fileObjectKey;
        this.externalUrl = externalUrl;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadedByUser = uploadedByUser;
        this.status = EvidenceStatus.ACTIVE;
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

    public void archive() {
        this.status = EvidenceStatus.ARCHIVED;
    }

    public void updateMetadata(
            String title,
            String description,
            EvidenceType evidenceType,
            String externalUrl
    ) {
        this.title = title.trim();
        this.description = description;
        this.evidenceType = evidenceType;
        this.externalUrl = externalUrl;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public EvidenceType getEvidenceType() {
        return evidenceType;
    }

    public EvidenceSourceType getSourceType() {
        return sourceType;
    }

    public String getFileObjectKey() {
        return fileObjectKey;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public User getUploadedByUser() {
        return uploadedByUser;
    }

    public EvidenceStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}