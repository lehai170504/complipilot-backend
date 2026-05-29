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
        name = "compliance_framework_translations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_compliance_framework_translations_framework_locale",
                        columnNames = {"framework_id", "locale"}
                )
        }
)
public class ComplianceFrameworkTranslation {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "framework_id", nullable = false)
    private ComplianceFramework framework;

    @Column(nullable = false, length = 10)
    private String locale;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ComplianceFrameworkTranslation() {
    }

    public ComplianceFrameworkTranslation(
            ComplianceFramework framework,
            String locale,
            String name,
            String description
    ) {
        this.id = UUID.randomUUID();
        this.framework = framework;
        this.locale = locale.trim().toLowerCase();
        this.name = name.trim();
        this.description = description;
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

    public void updateText(String name, String description) {
        this.name = name.trim();
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public ComplianceFramework getFramework() {
        return framework;
    }

    public String getLocale() {
        return locale;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
