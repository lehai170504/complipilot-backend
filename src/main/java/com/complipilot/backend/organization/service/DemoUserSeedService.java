package com.complipilot.backend.organization.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;
import com.complipilot.backend.organization.dto.OrganizationMemberResponse;
import com.complipilot.backend.organization.dto.SeedDemoUsersResponse;
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
public class DemoUserSeedService {

    private static final String DEMO_PASSWORD = "Password123!";

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantAccessService tenantAccessService;
    private final OrganizationMemberManagementService memberManagementService;

    public DemoUserSeedService(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TenantAccessService tenantAccessService,
            OrganizationMemberManagementService memberManagementService
    ) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantAccessService = tenantAccessService;
        this.memberManagementService = memberManagementService;
    }

    @Transactional
    public SeedDemoUsersResponse seedDemoUsers(UUID organizationId, UUID actorUserId) {
        tenantAccessService.requireOwnerRole(organizationId, actorUserId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        int createdUsers = 0;
        int createdMemberships = 0;
        int updatedMemberships = 0;
        List<OrganizationMemberResponse> members = new ArrayList<>();

        for (DemoUser demoUser : demoUsers()) {
            User user = userRepository.findByEmailIgnoreCase(demoUser.email())
                    .orElse(null);

            if (user == null) {
                user = userRepository.save(new User(
                        demoUser.email(),
                        passwordEncoder.encode(DEMO_PASSWORD),
                        demoUser.fullName()
                ));
                createdUsers++;
            }

            OrganizationMember member = organizationMemberRepository
                    .findByOrganizationIdAndUserId(organizationId, user.getId())
                    .orElse(null);

            if (member == null) {
                member = organizationMemberRepository.save(new OrganizationMember(
                        organization,
                        user,
                        demoUser.role()
                ));
                createdMemberships++;
            } else {
                member.changeRole(demoUser.role());
                member.changeStatus(OrganizationMemberStatus.ACTIVE);
                member = organizationMemberRepository.save(member);
                updatedMemberships++;
            }

            members.add(memberManagementService.toResponse(member));
        }

        return new SeedDemoUsersResponse(
                createdUsers,
                createdMemberships,
                updatedMemberships,
                members
        );
    }

    private List<DemoUser> demoUsers() {
        return List.of(
                new DemoUser("owner@complipilot.dev", "Demo Owner", OrganizationMemberRole.OWNER),
                new DemoUser("admin@complipilot.dev", "Demo Admin", OrganizationMemberRole.ADMIN),
                new DemoUser("manager@complipilot.dev", "Demo Compliance Manager", OrganizationMemberRole.COMPLIANCE_MANAGER),
                new DemoUser("member@complipilot.dev", "Demo Member", OrganizationMemberRole.MEMBER),
                new DemoUser("auditor@complipilot.dev", "Demo Auditor", OrganizationMemberRole.AUDITOR)
        );
    }

    private record DemoUser(
            String email,
            String fullName,
            OrganizationMemberRole role
    ) {
    }
}
