package com.complipilot.backend.evidence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.organization.entity.Organization;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "evidence_ai_analyses")
public class EvidenceAiAnalysis {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evidence_document_id", nullable = false)
    private EvidenceDocument evidenceDocument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analyzed_by_user_id", nullable = false)
    private User analyzedByUser;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "risk_level", nullable = false, length = 50)
    private String riskLevel;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> findings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_information", nullable = false, columnDefinition = "jsonb")
    private List<String> missingInformation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suggested_actions", nullable = false, columnDefinition = "jsonb")
    private List<String> suggestedActions;

    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt;

    protected EvidenceAiAnalysis() {
    }

    public EvidenceAiAnalysis(
            Organization organization,
            EvidenceDocument evidenceDocument,
            User analyzedByUser,
            String summary,
            String riskLevel,
            BigDecimal confidence,
            List<String> findings,
            List<String> missingInformation,
            List<String> suggestedActions
    ) {
        this.id = UUID.randomUUID();
        this.organization = organization;
        this.evidenceDocument = evidenceDocument;
        this.analyzedByUser = analyzedByUser;
        this.summary = summary;
        this.riskLevel = riskLevel;
        this.confidence = confidence;
        this.findings = findings == null ? List.of() : findings;
        this.missingInformation = missingInformation == null ? List.of() : missingInformation;
        this.suggestedActions = suggestedActions == null ? List.of() : suggestedActions;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }

        if (this.analyzedAt == null) {
            this.analyzedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public EvidenceDocument getEvidenceDocument() {
        return evidenceDocument;
    }

    public User getAnalyzedByUser() {
        return analyzedByUser;
    }

    public String getSummary() {
        return summary;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public List<String> getFindings() {
        return findings;
    }

    public List<String> getMissingInformation() {
        return missingInformation;
    }

    public List<String> getSuggestedActions() {
        return suggestedActions;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }
}