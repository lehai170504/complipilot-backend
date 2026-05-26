package com.complipilot.backend.organization.service;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.complipilot.backend.common.error.ForbiddenException;

import com.complipilot.backend.organization.entity.OrganizationMember;
import com.complipilot.backend.organization.enums.OrganizationMemberRole;
import com.complipilot.backend.organization.enums.OrganizationMemberStatus;
import com.complipilot.backend.organization.repository.OrganizationMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantAccessService {

    private final OrganizationMemberRepository organizationMemberRepository;

    public TenantAccessService(
            OrganizationMemberRepository organizationMemberRepository
    ) {
        this.organizationMemberRepository = organizationMemberRepository;
    }

    @Transactional(readOnly = true)
    public OrganizationMember requireActiveMember(
            UUID organizationId,
            UUID userId
    ) {
        return organizationMemberRepository
                .findByOrganization_IdAndUser_IdAndStatus(
                        organizationId,
                        userId,
                        OrganizationMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new ForbiddenException(
                        "You do not have access to this organization"
                ));
    }

    @Transactional(readOnly = true)
    public OrganizationMember requireAnyRole(
            UUID organizationId,
            UUID userId,
            OrganizationMemberRole... allowedRoles
    ) {
        OrganizationMember member = requireActiveMember(organizationId, userId);

        Set<OrganizationMemberRole> allowedRoleSet = Arrays.stream(allowedRoles)
                .collect(Collectors.toSet());

        if (!allowedRoleSet.contains(member.getRole())) {
            throw new ForbiddenException("You do not have permission to perform this action");
        }

        return member;
    }

    @Transactional(readOnly = true)
    public OrganizationMember requireManagerRole(
            UUID organizationId,
            UUID userId
    ) {
        return requireAnyRole(
                organizationId,
                userId,
                OrganizationMemberRole.OWNER,
                OrganizationMemberRole.ADMIN,
                OrganizationMemberRole.COMPLIANCE_MANAGER
        );
    }

    @Transactional(readOnly = true)
    public OrganizationMember requireAdminRole(
            UUID organizationId,
            UUID userId
    ) {
        return requireAnyRole(
                organizationId,
                userId,
                OrganizationMemberRole.OWNER,
                OrganizationMemberRole.ADMIN
        );
    }

    @Transactional(readOnly = true)
    public OrganizationMember requireOwnerRole(
            UUID organizationId,
            UUID userId
    ) {
        return requireAnyRole(
                organizationId,
                userId,
                OrganizationMemberRole.OWNER
        );
    }
}