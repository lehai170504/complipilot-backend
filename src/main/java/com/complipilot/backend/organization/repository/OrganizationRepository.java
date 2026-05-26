package com.complipilot.backend.organization.repository;

import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);
}