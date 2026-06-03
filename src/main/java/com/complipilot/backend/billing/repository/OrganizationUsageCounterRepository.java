package com.complipilot.backend.billing.repository;

import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.billing.entity.OrganizationUsageCounter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationUsageCounterRepository extends JpaRepository<OrganizationUsageCounter, UUID> {

    Optional<OrganizationUsageCounter> findByOrganization_IdAndPeriodMonth(
            UUID organizationId,
            String periodMonth
    );
}
