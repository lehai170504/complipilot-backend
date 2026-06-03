package com.complipilot.backend.identity.service;

import com.complipilot.backend.auth.dto.LogoutRequest;
import com.complipilot.backend.auth.dto.RefreshTokenRequest;
import com.complipilot.backend.billing.service.UsageQuotaService;
import com.complipilot.backend.common.error.ConflictException;
import com.complipilot.backend.common.error.UnauthorizedException;
import com.complipilot.backend.common.security.JwtService;
import com.complipilot.backend.common.util.SlugUtils;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;
import com.complipilot.backend.identity.dto.AuthUserResponse;
import com.complipilot.backend.identity.dto.login.LoginRequest;
import com.complipilot.backend.identity.dto.login.LoginResponse;
import com.complipilot.backend.identity.dto.register.RegisterRequest;
import com.complipilot.backend.identity.dto.register.RegisterResponse;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.entity.OrganizationMember;
import com.complipilot.backend.organization.repository.OrganizationMemberRepository;
import com.complipilot.backend.organization.enums.OrganizationMemberRole;
import com.complipilot.backend.organization.repository.OrganizationRepository;
import com.complipilot.backend.common.security.refresh.RefreshTokenService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UsageQuotaService usageQuotaService;

    public AuthService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UsageQuotaService usageQuotaService
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.usageQuotaService = usageQuotaService;
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

        usageQuotaService.createDefaultSubscription(organization);

        return new RegisterResponse(
                user.getId(),
                organization.getId(),
                user.getEmail(),
                user.getFullName(),
                organization.getName(),
                OrganizationMemberRole.OWNER
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                new AuthUserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getFullName()
                )
        );
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        User user = refreshTokenService.validateAndGetUser(request.refreshToken());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        refreshTokenService.revoke(request.refreshToken());

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                new AuthUserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getFullName()
                )
        );
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
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
