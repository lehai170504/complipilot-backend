package com.complipilot.backend.organization.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.complipilot.backend.common.error.BadRequestException;
import com.complipilot.backend.common.error.ConflictException;
import com.complipilot.backend.common.error.ForbiddenException;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;
import com.complipilot.backend.organization.dto.CreateOrganizationMemberRequest;
import com.complipilot.backend.organization.dto.OrganizationMemberResponse;
import com.complipilot.backend.organization.dto.UpdateOrganizationMemberRoleRequest;
import com.complipilot.backend.organization.dto.UpdateOrganizationMemberStatusRequest;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.entity.OrganizationMember;
import com.complipilot.backend.organization.enums.OrganizationMemberRole;
import com.complipilot.backend.organization.enums.OrganizationMemberStatus;
import com.complipilot.backend.organization.repository.OrganizationMemberRepository;
import com.complipilot.backend.organization.repository.OrganizationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationMemberManagementService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantAccessService tenantAccessService;

    public OrganizationMemberManagementService(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TenantAccessService tenantAccessService
    ) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantAccessService = tenantAccessService;
    }

    @Transactional(readOnly = true)
    public List<OrganizationMemberResponse> listMembers(UUID organizationId, UUID actorUserId) {
        tenantAccessService.requireActiveMember(organizationId, actorUserId);

        return organizationMemberRepository.findByOrganization_Id(organizationId)
                .stream()
                .sorted(Comparator
                        .comparing((OrganizationMember member) -> member.getRole().name())
                        .thenComparing(member -> member.getUser().getEmail()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrganizationMemberResponse createMember(
            UUID organizationId,
            UUID actorUserId,
            CreateOrganizationMemberRequest request
    ) {
        OrganizationMember actorMember = tenantAccessService.requireAdminRole(organizationId, actorUserId);
        validateAssignableRole(actorMember, request.role());

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        String normalizedEmail = request.email().toLowerCase().trim();
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> userRepository.save(new User(
                        normalizedEmail,
                        passwordEncoder.encode(request.password()),
                        request.fullName()
                )));

        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(organizationId, user.getId())
                .orElse(null);

        if (member != null && member.getStatus() == OrganizationMemberStatus.ACTIVE) {
            throw new ConflictException("User is already an active member of this organization");
        }

        if (member == null) {
            member = new OrganizationMember(organization, user, request.role());
        } else {
            member.changeRole(request.role());
            member.changeStatus(OrganizationMemberStatus.ACTIVE);
        }

        return toResponse(organizationMemberRepository.save(member));
    }

    @Transactional
    public OrganizationMemberResponse updateRole(
            UUID organizationId,
            UUID memberId,
            UUID actorUserId,
            UpdateOrganizationMemberRoleRequest request
    ) {
        OrganizationMember actorMember = tenantAccessService.requireAdminRole(organizationId, actorUserId);
        OrganizationMember targetMember = findOrganizationMember(organizationId, memberId);

        validateNotChangingOwnOwnerRole(actorMember, targetMember, request.role());
        validateAssignableRole(actorMember, request.role());
        validateOwnerSafety(targetMember, request.role(), targetMember.getStatus());

        targetMember.changeRole(request.role());

        return toResponse(organizationMemberRepository.save(targetMember));
    }

    @Transactional
    public OrganizationMemberResponse updateStatus(
            UUID organizationId,
            UUID memberId,
            UUID actorUserId,
            UpdateOrganizationMemberStatusRequest request
    ) {
        OrganizationMember actorMember = tenantAccessService.requireAdminRole(organizationId, actorUserId);
        OrganizationMember targetMember = findOrganizationMember(organizationId, memberId);

        if (targetMember.getUser().getId().equals(actorUserId)) {
            throw new BadRequestException("You cannot change your own membership status");
        }

        if (targetMember.getRole() == OrganizationMemberRole.OWNER && actorMember.getRole() != OrganizationMemberRole.OWNER) {
            throw new ForbiddenException("Only an owner can change another owner's status");
        }

        validateOwnerSafety(targetMember, targetMember.getRole(), request.status());
        targetMember.changeStatus(request.status());

        return toResponse(organizationMemberRepository.save(targetMember));
    }

    @Transactional
    public void removeMember(UUID organizationId, UUID memberId, UUID actorUserId) {
        OrganizationMember actorMember = tenantAccessService.requireAdminRole(organizationId, actorUserId);
        OrganizationMember targetMember = findOrganizationMember(organizationId, memberId);

        if (targetMember.getUser().getId().equals(actorUserId)) {
            throw new BadRequestException("You cannot remove yourself from the organization");
        }

        if (targetMember.getRole() == OrganizationMemberRole.OWNER && actorMember.getRole() != OrganizationMemberRole.OWNER) {
            throw new ForbiddenException("Only an owner can remove another owner");
        }

        validateOwnerSafety(targetMember, targetMember.getRole(), OrganizationMemberStatus.DISABLED);
        targetMember.changeStatus(OrganizationMemberStatus.DISABLED);
        organizationMemberRepository.save(targetMember);
    }

    private OrganizationMember findOrganizationMember(UUID organizationId, UUID memberId) {
        OrganizationMember member = organizationMemberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Organization member not found"));

        if (!member.getOrganization().getId().equals(organizationId)) {
            throw new NotFoundException("Organization member not found");
        }

        return member;
    }

    private void validateAssignableRole(OrganizationMember actorMember, OrganizationMemberRole targetRole) {
        if (targetRole == OrganizationMemberRole.OWNER && actorMember.getRole() != OrganizationMemberRole.OWNER) {
            throw new ForbiddenException("Only an owner can assign the owner role");
        }
    }

    private void validateNotChangingOwnOwnerRole(
            OrganizationMember actorMember,
            OrganizationMember targetMember,
            OrganizationMemberRole requestedRole
    ) {
        if (targetMember.getUser().getId().equals(actorMember.getUser().getId())
                && actorMember.getRole() == OrganizationMemberRole.OWNER
                && requestedRole != OrganizationMemberRole.OWNER) {
            throw new BadRequestException("You cannot remove your own owner role");
        }
    }

    private void validateOwnerSafety(
            OrganizationMember targetMember,
            OrganizationMemberRole requestedRole,
            OrganizationMemberStatus requestedStatus
    ) {
        boolean targetStopsBeingActiveOwner = targetMember.getRole() == OrganizationMemberRole.OWNER
                && targetMember.getStatus() == OrganizationMemberStatus.ACTIVE
                && (requestedRole != OrganizationMemberRole.OWNER
                || requestedStatus != OrganizationMemberStatus.ACTIVE);

        if (!targetStopsBeingActiveOwner) {
            return;
        }

        long activeOwners = organizationMemberRepository.countByOrganization_IdAndRoleAndStatus(
                targetMember.getOrganization().getId(),
                OrganizationMemberRole.OWNER,
                OrganizationMemberStatus.ACTIVE
        );

        if (activeOwners <= 1) {
            throw new BadRequestException("An organization must have at least one active owner");
        }
    }

    public OrganizationMemberResponse toResponse(OrganizationMember member) {
        return new OrganizationMemberResponse(
                member.getId(),
                member.getOrganization().getId(),
                member.getUser().getId(),
                member.getUser().getEmail(),
                member.getUser().getFullName(),
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
