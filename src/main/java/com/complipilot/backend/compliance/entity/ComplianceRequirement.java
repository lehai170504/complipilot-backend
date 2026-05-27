package com.complipilot.backend.compliance.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "compliance_requirements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_compliance_requirements_framework_code",
                        columnNames = {"framework_id", "code"}
                )
        }
)
public class ComplianceRequirement {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "framework_id", nullable = false)
    private ComplianceFramework framework;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 120)
    private String category;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ComplianceRequirement() {
    }

    public ComplianceRequirement(
            ComplianceFramework framework,
            String code,
            String title,
            String description,
            String category,
            int sortOrder
    ) {
        this.id = UUID.randomUUID();
        this.framework = framework;
        this.code = code.trim().toUpperCase();
        this.title = title.trim();
        this.description = description;
        this.category = category;
        this.sortOrder = sortOrder;
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

    public UUID getId() {
        return id;
    }

    public ComplianceFramework getFramework() {
        return framework;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}