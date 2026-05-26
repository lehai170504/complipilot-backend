package com.complipilot.backend.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complipilot.backend.common.error.ForbiddenException;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;

import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.entity.OrganizationMember;
import com.complipilot.backend.organization.enums.OrganizationMemberRole;
import com.complipilot.backend.organization.repository.OrganizationMemberRepository;
import com.complipilot.backend.organization.repository.OrganizationRepository;
import com.complipilot.backend.organization.service.TenantAccessService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Import(TenantAccessServiceTest.TestcontainersConfig.class)
class TenantAccessServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository organizationMemberRepository;

    @Autowired
    private TenantAccessService tenantAccessService;

    @Test
    void shouldAllowActiveMemberAccess() {
        User user = userRepository.save(
                new User(
                        "member-access@example.com",
                        "hashed-password",
                        "Member Access"
                )
        );

        Organization organization = organizationRepository.save(
                new Organization(
                        "Member Access Company",
                        "member-access-company"
                )
        );

        organizationMemberRepository.save(
                new OrganizationMember(
                        organization,
                        user,
                        OrganizationMemberRole.MEMBER
                )
        );

        OrganizationMember member = tenantAccessService.requireActiveMember(
                organization.getId(),
                user.getId()
        );

        assertThat(member.getUser().getId()).isEqualTo(user.getId());
        assertThat(member.getOrganization().getId()).isEqualTo(organization.getId());
        assertThat(member.getRole()).isEqualTo(OrganizationMemberRole.MEMBER);
    }

    @Test
    void shouldRejectUserWithoutMembership() {
        User user = userRepository.save(
                new User(
                        "outside-user@example.com",
                        "hashed-password",
                        "Outside User"
                )
        );

        Organization organization = organizationRepository.save(
                new Organization(
                        "Private Company",
                        "private-company"
                )
        );

        assertThatThrownBy(() ->
                tenantAccessService.requireActiveMember(
                        organization.getId(),
                        user.getId()
                )
        )
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You do not have access to this organization");
    }

    @Test
    void shouldAllowManagerRoleForOwner() {
        User user = userRepository.save(
                new User(
                        "owner-role@example.com",
                        "hashed-password",
                        "Owner Role"
                )
        );

        Organization organization = organizationRepository.save(
                new Organization(
                        "Owner Role Company",
                        "owner-role-company"
                )
        );

        organizationMemberRepository.save(
                new OrganizationMember(
                        organization,
                        user,
                        OrganizationMemberRole.OWNER
                )
        );

        OrganizationMember member = tenantAccessService.requireManagerRole(
                organization.getId(),
                user.getId()
        );

        assertThat(member.getRole()).isEqualTo(OrganizationMemberRole.OWNER);
    }

    @Test
    void shouldRejectMemberForAdminRole() {
        User user = userRepository.save(
                new User(
                        "member-role@example.com",
                        "hashed-password",
                        "Member Role"
                )
        );

        Organization organization = organizationRepository.save(
                new Organization(
                        "Member Role Company",
                        "member-role-company"
                )
        );

        organizationMemberRepository.save(
                new OrganizationMember(
                        organization,
                        user,
                        OrganizationMemberRole.MEMBER
                )
        );

        assertThatThrownBy(() ->
                tenantAccessService.requireAdminRole(
                        organization.getId(),
                        user.getId()
                )
        )
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You do not have permission to perform this action");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestcontainersConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:16-alpine")
            );
        }
    }
}