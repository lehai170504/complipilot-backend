package com.complipilot.backend.compliance.repository;

import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.compliance.entity.ComplianceFramework;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceFrameworkRepository extends JpaRepository<ComplianceFramework, UUID> {

    Optional<ComplianceFramework> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}