package com.complipilot.backend.compliance.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.compliance.entity.CompanyComplianceItem;
import com.complipilot.backend.compliance.enums.CompanyComplianceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
        SELECT item
        FROM CompanyComplianceItem item
        JOIN item.requirement req
        WHERE item.organization.id = :organizationId
          AND (
                :query IS NULL
                OR LOWER(req.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(req.description) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(item.notes) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        """)
    Page<CompanyComplianceItem> searchByOrganizationIdAndQuery(
            @Param("organizationId") UUID organizationId,
            @Param("query") String query,
            Pageable pageable
    );
}