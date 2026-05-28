package com.complipilot.backend.task.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.complipilot.backend.task.entity.ComplianceTask;
import com.complipilot.backend.task.enums.ComplianceTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
