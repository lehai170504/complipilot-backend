package com.complipilot.backend.compliance.repository;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.compliance.entity.ComplianceRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceRequirementRepository extends JpaRepository<ComplianceRequirement, UUID> {

    List<ComplianceRequirement> findByFramework_IdOrderBySortOrderAsc(UUID frameworkId);
}