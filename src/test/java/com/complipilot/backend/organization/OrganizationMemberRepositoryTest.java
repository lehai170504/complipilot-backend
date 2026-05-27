package com.complipilot.backend.organization;

import static org.assertj.core.api.Assertions.assertThat;

import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;

import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.entity.OrganizationMember;
import com.complipilot.backend.organization.enums.OrganizationMemberRole;
import com.complipilot.backend.organization.repository.OrganizationMemberRepository;
import com.complipilot.backend.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
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

}
