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
        name = "compliance_requirement_translations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_compliance_requirement_translations_requirement_locale",
                        columnNames = {"requirement_id", "locale"}
                )
        }
)
public class ComplianceRequirementTranslation {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id", nullable = false)
    private ComplianceRequirement requirement;

    @Column(nullable = false, length = 10)
    private String locale;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 120)
    private String category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ComplianceRequirementTranslation() {
    }

    public ComplianceRequirementTranslation(
            ComplianceRequirement requirement,
            String locale,
            String title,
            String description,
            String category
    ) {
        this.id = UUID.randomUUID();
        this.requirement = requirement;
        this.locale = locale.trim().toLowerCase();
        this.title = title.trim();
        this.description = description;
        this.category = category;
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

    public void updateText(String title, String description, String category) {
        this.title = title.trim();
        this.description = description;
        this.category = category;
    }

    public UUID getId() {
        return id;
    }

    public ComplianceRequirement getRequirement() {
        return requirement;
    }

    public String getLocale() {
        return locale;
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
}
