package com.complipilot.backend.task.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.task.enums.ComplianceTaskPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.complipilot.backend.task.entity.ComplianceTask;
import com.complipilot.backend.task.enums.ComplianceTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComplianceTaskRepository extends JpaRepository<ComplianceTask, UUID> {

    List<ComplianceTask> findByOrganization_IdOrderByCreatedAtDesc(UUID organizationId);

    Page<ComplianceTask> findByOrganization_Id(UUID organizationId, Pageable pageable);

    Optional<ComplianceTask> findByIdAndOrganization_Id(
            UUID id,
            UUID organizationId
    );

    List<ComplianceTask> findByOrganization_IdAndAssigneeUser_IdOrderByCreatedAtDesc(
            UUID organizationId,
            UUID assigneeUserId
    );

    List<ComplianceTask> findByOrganization_IdAndStatusOrderByCreatedAtDesc(
            UUID organizationId,
            ComplianceTaskStatus status
    );

    long countByOrganization_Id(UUID organizationId);

    long countByOrganization_IdAndStatus(
            UUID organizationId,
            ComplianceTaskStatus status
    );

    List<ComplianceTask> findByOrganization_IdAndDueDateBeforeAndStatusNotInOrderByDueDateAsc(
            UUID organizationId,
            LocalDate date,
            Collection<ComplianceTaskStatus> excludedStatuses
    );

    @Query("""
        SELECT task
        FROM ComplianceTask task
        WHERE task.organization.id = :organizationId
          AND (:status IS NULL OR task.status = :status)
          AND (:priority IS NULL OR task.priority = :priority)
          AND (:assigneeUserId IS NULL OR task.assigneeUser.id = :assigneeUserId)
          AND (:complianceItemId IS NULL OR task.complianceItem.id = :complianceItemId)
        """)
    Page<ComplianceTask> findByOrganizationIdWithFilters(
            @Param("organizationId") UUID organizationId,
            @Param("status") ComplianceTaskStatus status,
            @Param("priority") ComplianceTaskPriority priority,
            @Param("assigneeUserId") UUID assigneeUserId,
            @Param("complianceItemId") UUID complianceItemId,
            Pageable pageable
    );

    @Query("""
        SELECT task
        FROM ComplianceTask task
        WHERE task.organization.id = :organizationId
          AND (:status IS NULL OR task.status = :status)
          AND (:priority IS NULL OR task.priority = :priority)
          AND (:assigneeUserId IS NULL OR task.assigneeUser.id = :assigneeUserId)
          AND (:complianceItemId IS NULL OR task.complianceItem.id = :complianceItemId)
          AND (
                :query IS NULL
                OR LOWER(task.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(task.description) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        """)
    Page<ComplianceTask> findByOrganizationIdWithFilters(
            @Param("organizationId") UUID organizationId,
            @Param("status") ComplianceTaskStatus status,
            @Param("priority") ComplianceTaskPriority priority,
            @Param("assigneeUserId") UUID assigneeUserId,
            @Param("complianceItemId") UUID complianceItemId,
            @Param("query") String query,
            Pageable pageable
    );
}
