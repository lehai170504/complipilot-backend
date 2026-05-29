package com.complipilot.backend.compliance.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.compliance.entity.ComplianceFrameworkTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceFrameworkTranslationRepository
        extends JpaRepository<ComplianceFrameworkTranslation, UUID> {

    Optional<ComplianceFrameworkTranslation> findByFramework_IdAndLocale(
            UUID frameworkId,
            String locale
    );

    List<ComplianceFrameworkTranslation> findByFramework_IdInAndLocale(
            Collection<UUID> frameworkIds,
            String locale
    );
}
