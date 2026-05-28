package com.complipilot.backend.compliance.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.compliance.entity.CompanyComplianceItem;
import com.complipilot.backend.compliance.enums.CompanyComplianceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyComplianceItemRepository extends JpaRepository<CompanyComplianceItem, UUID> {

    List<CompanyComplianceItem> findByOrganization_Id(UUID organizationId);

    Optional<CompanyComplianceItem> findByIdAndOrganization_Id(
            UUID id,
            UUID organizationId
    );

    boolean existsByOrganization_IdAndRequirement_Id(
            UUID organizationId,
            UUID requirementId
    );

    long countByOrganization_Id(UUID organizationId);

    long countByOrganization_IdAndStatus(
            UUID organizationId,
            CompanyComplianceStatus status
    );

    List<CompanyComplianceItem> findByOrganization_IdAndDueDateBetweenAndStatusNotInOrderByDueDateAsc(
            UUID organizationId,
            LocalDate startDate,
            LocalDate endDate,
            Collection<CompanyComplianceStatus> excludedStatuses
    );

    List<CompanyComplianceItem> findByOrganization_IdAndDueDateBeforeAndStatusNotInOrderByDueDateAsc(
            UUID organizationId,
            LocalDate date,
            Collection<CompanyComplianceStatus> excludedStatuses
    );
}