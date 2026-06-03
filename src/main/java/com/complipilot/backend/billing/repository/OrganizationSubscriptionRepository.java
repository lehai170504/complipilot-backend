package com.complipilot.backend.billing.repository;

import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.billing.entity.OrganizationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationSubscriptionRepository extends JpaRepository<OrganizationSubscription, UUID> {

    Optional<OrganizationSubscription> findByOrganization_Id(UUID organizationId);
}
