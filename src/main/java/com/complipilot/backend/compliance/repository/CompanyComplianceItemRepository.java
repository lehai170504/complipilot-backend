package com.complipilot.backend.compliance.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.compliance.entity.CompanyComplianceItem;
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
}