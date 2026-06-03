package com.complipilot.backend.common.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.complipilot.backend.common.error.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PlatformAdminService {

    private final Set<String> platformAdminEmails;

    public PlatformAdminService(
            @Value("${app.platform.admin-emails:}") String adminEmails
    ) {
        this.platformAdminEmails = Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void requirePlatformAdmin(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.email() == null) {
            throw new ForbiddenException("Platform admin access is required");
        }

        String normalizedEmail = authenticatedUser.email().trim().toLowerCase();

        if (!platformAdminEmails.contains(normalizedEmail)) {
            throw new ForbiddenException("Platform admin access is required");
        }
    }
}