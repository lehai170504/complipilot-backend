package com.complipilot.backend.organization.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.organization.entity.OrganizationInvitation;
import com.complipilot.backend.organization.enums.OrganizationInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitation, UUID> {

    List<OrganizationInvitation> findByOrganization_IdOrderByCreatedAtDesc(UUID organizationId);

    Optional<OrganizationInvitation> findByIdAndOrganization_Id(
            UUID id,
            UUID organizationId
    );

    Optional<OrganizationInvitation> findByTokenHash(String tokenHash);

    Optional<OrganizationInvitation> findByOrganization_IdAndEmailIgnoreCaseAndStatus(
            UUID organizationId,
            String email,
            OrganizationInvitationStatus status
    );

    boolean existsByOrganization_IdAndEmailIgnoreCaseAndStatus(
            UUID organizationId,
            String email,
            OrganizationInvitationStatus status
    );
}
