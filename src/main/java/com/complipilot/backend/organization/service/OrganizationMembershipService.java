package com.complipilot.backend.organization.service;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.organization.dto.OrganizationMembershipResponse;

import com.complipilot.backend.organization.enums.OrganizationMemberStatus;
import com.complipilot.backend.organization.repository.OrganizationMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationMembershipService {

    private final OrganizationMemberRepository organizationMemberRepository;

    public OrganizationMembershipService(
            OrganizationMemberRepository organizationMemberRepository
    ) {
        this.organizationMemberRepository = organizationMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizationMembershipResponse> findActiveMemberships(UUID userId) {
        return organizationMemberRepository
                .findByUser_IdAndStatus(userId, OrganizationMemberStatus.ACTIVE)
                .stream()
                .map(member -> new OrganizationMembershipResponse(
                        member.getOrganization().getId(),
                        member.getOrganization().getName(),
                        member.getOrganization().getSlug(),
                        member.getRole(),
                        member.getStatus()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isActiveMember(UUID organizationId, UUID userId) {
        return organizationMemberRepository.existsByOrganization_IdAndUser_IdAndStatus(
                organizationId,
                userId,
                OrganizationMemberStatus.ACTIVE
        );
    }
}