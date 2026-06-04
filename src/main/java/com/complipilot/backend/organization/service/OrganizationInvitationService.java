package com.complipilot.backend.organization.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import com.complipilot.backend.billing.service.UsageQuotaService;
import com.complipilot.backend.common.error.BadRequestException;
import com.complipilot.backend.common.error.ConflictException;
import com.complipilot.backend.common.error.ForbiddenException;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;
import com.complipilot.backend.organization.dto.AcceptOrganizationInvitationRequest;
import com.complipilot.backend.organization.dto.CreateOrganizationInvitationRequest;
import com.complipilot.backend.organization.dto.OrganizationInvitationResponse;
import com.complipilot.backend.organization.dto.OrganizationMemberResponse;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.entity.OrganizationInvitation;
import com.complipilot.backend.organization.entity.OrganizationMember;
import com.complipilot.backend.organization.enums.OrganizationInvitationStatus;
import com.complipilot.backend.organization.enums.OrganizationMemberRole;
import com.complipilot.backend.organization.enums.OrganizationMemberStatus;
import com.complipilot.backend.organization.repository.OrganizationInvitationRepository;
import com.complipilot.backend.organization.repository.OrganizationMemberRepository;
import com.complipilot.backend.organization.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationInvitationService {

    private static final Duration INVITATION_TTL = Duration.ofDays(7);

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantAccessService tenantAccessService;
    private final UsageQuotaService usageQuotaService;
    private final String frontendBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public OrganizationInvitationService(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            OrganizationInvitationRepository invitationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TenantAccessService tenantAccessService,
            UsageQuotaService usageQuotaService,
            @Value("${app.frontend.base-url:http://localhost:3000}") String frontendBaseUrl
    ) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantAccessService = tenantAccessService;
        this.usageQuotaService = usageQuotaService;
        this.frontendBaseUrl = normalizeBaseUrl(frontendBaseUrl);
    }

    @Transactional
    public OrganizationInvitationResponse createInvitation(
            UUID organizationId,
            UUID actorUserId,
            CreateOrganizationInvitationRequest request
    ) {
        OrganizationMember actorMember = tenantAccessService.requireAdminRole(
                organizationId,
                actorUserId
        );

        validateAssignableRole(actorMember, request.role());
        usageQuotaService.requireCanAddMember(organizationId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        User invitedByUser = userRepository.findById(actorUserId)
                .orElseThrow(() -> new NotFoundException("Inviting user not found"));

        String normalizedEmail = normalizeEmail(request.email());

        User existingUser = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElse(null);

        if (existingUser != null && organizationMemberRepository
                .existsByOrganization_IdAndUser_IdAndStatus(
                        organizationId,
                        existingUser.getId(),
                        OrganizationMemberStatus.ACTIVE
                )) {
            throw new ConflictException("User is already an active member of this organization");
        }

        if (invitationRepository.existsByOrganization_IdAndEmailIgnoreCaseAndStatus(
                organizationId,
                normalizedEmail,
                OrganizationInvitationStatus.PENDING
        )) {
            throw new ConflictException("A pending invitation already exists for this email");
        }

        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);

        OrganizationInvitation invitation = invitationRepository.save(
                new OrganizationInvitation(
                        organization,
                        normalizedEmail,
                        request.role(),
                        tokenHash,
                        invitedByUser,
                        Instant.now().plus(INVITATION_TTL)
                )
        );

        return toResponse(invitation, rawToken);
    }

    @Transactional(readOnly = true)
    public List<OrganizationInvitationResponse> listInvitations(
            UUID organizationId,
            UUID actorUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, actorUserId);

        return invitationRepository.findByOrganization_IdOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(invitation -> toResponse(invitation, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationInvitationResponse getInvitationByToken(String token) {
        OrganizationInvitation invitation = invitationRepository.findByTokenHash(hashToken(token))
                .orElseThrow(() -> new NotFoundException("Organization invitation not found"));

        if (invitation.isPending() && invitation.isExpired()) {
            return toResponseWithExpiredStatus(invitation);
        }

        return toResponse(invitation, null);
    }

    @Transactional
    public OrganizationMemberResponse acceptInvitation(
            String token,
            AcceptOrganizationInvitationRequest request
    ) {
        OrganizationInvitation invitation = invitationRepository.findByTokenHash(hashToken(token))
                .orElseThrow(() -> new NotFoundException("Organization invitation not found"));

        if (!invitation.isPending()) {
            throw new ConflictException("Organization invitation is no longer pending");
        }

        if (invitation.isExpired()) {
            invitation.expire();
            throw new ConflictException("Organization invitation has expired");
        }

        String normalizedEmail = normalizeEmail(request.email());

        if (!invitation.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("Invitation email does not match the submitted email");
        }

        usageQuotaService.requireCanAddMember(invitation.getOrganization().getId());

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> userRepository.save(
                        new User(
                                normalizedEmail,
                                passwordEncoder.encode(request.password()),
                                request.fullName()
                        )
                ));

        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(
                        invitation.getOrganization().getId(),
                        user.getId()
                )
                .orElse(null);

        if (member != null && member.getStatus() == OrganizationMemberStatus.ACTIVE) {
            throw new ConflictException("User is already an active member of this organization");
        }

        if (member == null) {
            member = new OrganizationMember(
                    invitation.getOrganization(),
                    user,
                    invitation.getRole()
            );
        } else {
            member.changeRole(invitation.getRole());
            member.changeStatus(OrganizationMemberStatus.ACTIVE);
        }

        OrganizationMember savedMember = organizationMemberRepository.save(member);
        invitation.accept(user);

        return toMemberResponse(savedMember);
    }

    @Transactional
    public void revokeInvitation(
            UUID organizationId,
            UUID invitationId,
            UUID actorUserId
    ) {
        OrganizationMember actorMember = tenantAccessService.requireAdminRole(
                organizationId,
                actorUserId
        );

        OrganizationInvitation invitation = invitationRepository
                .findByIdAndOrganization_Id(invitationId, organizationId)
                .orElseThrow(() -> new NotFoundException("Organization invitation not found"));

        validateAssignableRole(actorMember, invitation.getRole());

        if (!invitation.isPending()) {
            throw new ConflictException("Only pending invitations can be revoked");
        }

        invitation.revoke();
    }

    @Transactional
    public OrganizationInvitationResponse regenerateInvitationLink(
            UUID organizationId,
            UUID invitationId,
            UUID actorUserId
    ) {
        OrganizationMember actorMember = tenantAccessService.requireAdminRole(
                organizationId,
                actorUserId
        );

        OrganizationInvitation invitation = invitationRepository
                .findByIdAndOrganization_Id(invitationId, organizationId)
                .orElseThrow(() -> new NotFoundException("Organization invitation not found"));

        validateAssignableRole(actorMember, invitation.getRole());

        if (!invitation.isPending()) {
            throw new ConflictException("Only pending invitations can be regenerated");
        }

        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);

        invitation.regenerateToken(
                tokenHash,
                Instant.now().plus(INVITATION_TTL)
        );

        return toResponse(invitation, rawToken);
    }

    private void validateAssignableRole(
            OrganizationMember actorMember,
            OrganizationMemberRole targetRole
    ) {
        if (targetRole == OrganizationMemberRole.OWNER
                && actorMember.getRole() != OrganizationMemberRole.OWNER) {
            throw new ForbiddenException("Only an owner can assign the owner role");
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }

        return email.toLowerCase().trim();
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:3000";
        }

        String normalized = value.trim();

        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private String buildAcceptUrl(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        return frontendBaseUrl + "/invite/" + token;
    }

    private OrganizationInvitationResponse toResponse(
            OrganizationInvitation invitation,
            String rawToken
    ) {
        UUID acceptedByUserId = invitation.getAcceptedByUser() == null
                ? null
                : invitation.getAcceptedByUser().getId();

        String acceptedByEmail = invitation.getAcceptedByUser() == null
                ? null
                : invitation.getAcceptedByUser().getEmail();

        return new OrganizationInvitationResponse(
                invitation.getId(),
                invitation.getOrganization().getId(),
                invitation.getOrganization().getName(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getInvitedByUser().getId(),
                invitation.getInvitedByUser().getEmail(),
                acceptedByUserId,
                acceptedByEmail,
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getCreatedAt(),
                invitation.getUpdatedAt(),
                rawToken,
                buildAcceptUrl(rawToken)
        );
    }

    private OrganizationInvitationResponse toResponseWithExpiredStatus(
            OrganizationInvitation invitation
    ) {
        return new OrganizationInvitationResponse(
                invitation.getId(),
                invitation.getOrganization().getId(),
                invitation.getOrganization().getName(),
                invitation.getEmail(),
                invitation.getRole(),
                OrganizationInvitationStatus.EXPIRED,
                invitation.getInvitedByUser().getId(),
                invitation.getInvitedByUser().getEmail(),
                null,
                null,
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getCreatedAt(),
                invitation.getUpdatedAt(),
                null,
                null
        );
    }

    private OrganizationMemberResponse toMemberResponse(OrganizationMember member) {
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
