package com.complipilot.backend.organization.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.identity.entity.User;

import com.complipilot.backend.organization.entity.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    List<OrganizationMember> findByUser(User user);

    List<OrganizationMember> findByUserId(UUID userId);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}