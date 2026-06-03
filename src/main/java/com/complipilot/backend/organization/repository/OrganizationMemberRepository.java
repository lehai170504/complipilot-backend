package com.complipilot.backend.organization.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.identity.entity.User;

import com.complipilot.backend.organization.entity.OrganizationMember;
import com.complipilot.backend.organization.enums.OrganizationMemberRole;
import com.complipilot.backend.organization.enums.OrganizationMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    List<OrganizationMember> findByUser(User user);

    List<OrganizationMember> findByUserId(UUID userId);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    Optional<OrganizationMember> findByIdAndOrganization_Id(
            UUID id,
            UUID organizationId
    );

    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    List<OrganizationMember> findByUser_IdAndStatus(
            UUID userId,
            OrganizationMemberStatus status
    );

    Optional<OrganizationMember> findByOrganization_IdAndUser_IdAndStatus(
            UUID organizationId,
            UUID userId,
            OrganizationMemberStatus status
    );

    boolean existsByOrganization_IdAndUser_IdAndStatus(
            UUID organizationId,
            UUID userId,
            OrganizationMemberStatus status
    );

    List<OrganizationMember> findByOrganization_Id(UUID organizationId);

    long countByOrganization_IdAndRoleAndStatus(
            UUID organizationId,
            OrganizationMemberRole role,
            OrganizationMemberStatus status
    );

}
