package com.complipilot.backend.identity;

import com.complipilot.backend.common.error.ConflictException;
import com.complipilot.backend.common.util.SlugUtils;
import com.complipilot.backend.identity.dto.RegisterRequest;
import com.complipilot.backend.identity.dto.RegisterResponse;
import com.complipilot.backend.organization.Organization;
import com.complipilot.backend.organization.OrganizationMember;
import com.complipilot.backend.organization.OrganizationMemberRepository;
import com.complipilot.backend.organization.OrganizationMemberRole;
import com.complipilot.backend.organization.OrganizationRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = userRepository.save(
                new User(
                        normalizedEmail,
                        passwordHash,
                        request.fullName()
                )
        );

        String baseSlug = SlugUtils.toSlug(request.organizationName());
        String finalSlug = generateUniqueSlug(baseSlug);

        Organization organization = organizationRepository.save(
                new Organization(
                        request.organizationName(),
                        finalSlug
                )
        );

        organizationMemberRepository.save(
                new OrganizationMember(
                        organization,
                        user,
                        OrganizationMemberRole.OWNER
                )
        );

        return new RegisterResponse(
                user.getId(),
                organization.getId(),
                user.getEmail(),
                user.getFullName(),
                organization.getName(),
                OrganizationMemberRole.OWNER
        );
    }

    private String generateUniqueSlug(String baseSlug) {
        if (!organizationRepository.existsBySlug(baseSlug)) {
            return baseSlug;
        }

        String candidate;

        do {
            candidate = SlugUtils.withRandomSuffix(baseSlug);
        } while (organizationRepository.existsBySlug(candidate));

        return candidate;
    }
}