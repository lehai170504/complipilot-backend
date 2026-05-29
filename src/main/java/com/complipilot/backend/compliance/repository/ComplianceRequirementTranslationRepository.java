package com.complipilot.backend.compliance.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.compliance.entity.ComplianceRequirementTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceRequirementTranslationRepository
        extends JpaRepository<ComplianceRequirementTranslation, UUID> {

    Optional<ComplianceRequirementTranslation> findByRequirement_IdAndLocale(
            UUID requirementId,
            String locale
    );

    List<ComplianceRequirementTranslation> findByRequirement_IdInAndLocale(
            Collection<UUID> requirementIds,
            String locale
    );
}
