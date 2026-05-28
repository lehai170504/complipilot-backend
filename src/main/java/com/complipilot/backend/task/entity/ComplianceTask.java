package com.complipilot.backend.task.entity;

import com.complipilot.backend.compliance.entity.CompanyComplianceItem;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.task.enums.ComplianceTaskPriority;
import com.complipilot.backend.task.enums.ComplianceTaskStatus;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "compliance_tasks")
public class ComplianceTask {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compliance_item_id")
    private CompanyComplianceItem complianceItem;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_user_id")
    private User assigneeUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ComplianceTaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ComplianceTaskPriority priority;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ComplianceTask() {
    }

    public ComplianceTask(
            Organization organization,
            CompanyComplianceItem complianceItem,
            String title,
            String description,
            User assigneeUser,
            User createdByUser,
            ComplianceTaskPriority priority,
            LocalDate dueDate
    ) {
        this.id = UUID.randomUUID();
        this.organization = organization;
        this.complianceItem = complianceItem;
        this.title = title.trim();
        this.description = description;
        this.assigneeUser = assigneeUser;
        this.createdByUser = createdByUser;
        this.status = ComplianceTaskStatus.OPEN;
        this.priority = priority == null ? ComplianceTaskPriority.MEDIUM : priority;
        this.dueDate = dueDate;
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

    public void updateDetails(
            String title,
            String description,
            User assigneeUser,
            ComplianceTaskPriority priority,
            LocalDate dueDate
    ) {
        this.title = title.trim();
        this.description = description;
        this.assigneeUser = assigneeUser;
        this.priority = priority == null ? this.priority : priority;
        this.dueDate = dueDate;
    }

    public void updateStatus(ComplianceTaskStatus status) {
        this.status = status;

        if (status == ComplianceTaskStatus.DONE && this.completedAt == null) {
            this.completedAt = Instant.now();
        }

        if (status != ComplianceTaskStatus.DONE) {
            this.completedAt = null;
        }
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public CompanyComplianceItem getComplianceItem() {
        return complianceItem;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public User getAssigneeUser() {
        return assigneeUser;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public ComplianceTaskStatus getStatus() {
        return status;
    }

    public ComplianceTaskPriority getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
