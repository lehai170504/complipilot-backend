package com.complipilot.backend.organization;

import static org.assertj.core.api.Assertions.assertThat;

import com.complipilot.backend.identity.User;
import com.complipilot.backend.identity.UserRepository;

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
@Import(OrganizationMemberRepositoryTest.TestcontainersConfig.class)
class OrganizationMemberRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository organizationMemberRepository;

    @Test
    void shouldCreateOrganizationMembership() {
        User user = userRepository.save(
                new User(
                        "hai@example.com",
                        "hashed-password",
                        "Lê Hoàng Hải"
                )
        );

        Organization organization = organizationRepository.save(
                new Organization(
                        "CompliPilot Demo Company",
                        "complipilot-demo-company"
                )
        );

        OrganizationMember member = organizationMemberRepository.save(
                new OrganizationMember(
                        organization,
                        user,
                        OrganizationMemberRole.OWNER
                )
        );

        assertThat(member.getId()).isNotNull();
        assertThat(member.getRole()).isEqualTo(OrganizationMemberRole.OWNER);
        assertThat(member.isActive()).isTrue();

        assertThat(
                organizationMemberRepository.existsByOrganizationIdAndUserId(
                        organization.getId(),
                        user.getId()
                )
        ).isTrue();
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