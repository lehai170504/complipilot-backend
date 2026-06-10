package com.complipilot.backend.billing.repository;

import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.billing.entity.BillingPlanChangeRequest;
import com.complipilot.backend.billing.enums.BillingPlanChangeRequestStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingPlanChangeRequestRepository
        extends JpaRepository<BillingPlanChangeRequest, UUID> {

    Optional<BillingPlanChangeRequest>
    findTopByOrganization_IdOrderByCreatedAtDesc(UUID organizationId);

    boolean existsByOrganization_IdAndStatus(
            UUID organizationId,
            BillingPlanChangeRequestStatus status
    );

    Page<BillingPlanChangeRequest> findByStatusOrderByCreatedAtDesc(
            BillingPlanChangeRequestStatus status,
            Pageable pageable
    );

    Page<BillingPlanChangeRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<BillingPlanChangeRequest> findByOrganization_IdOrderByCreatedAtDesc(
            UUID organizationId,
            Pageable pageable
    );

    Optional<BillingPlanChangeRequest> findByIdAndOrganization_Id(
            UUID id,
            UUID organizationId
    );
}