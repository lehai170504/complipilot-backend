package com.complipilot.backend.organization.controller;

import java.util.UUID;

import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.organization.dto.SeedDemoUsersResponse;
import com.complipilot.backend.organization.service.DemoUserSeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Demo Data", description = "Local/demo-only data seed APIs")
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/members")
@ConditionalOnProperty(name = "app.demo-users.enabled", havingValue = "true")
public class DemoUserSeedController {

    private final DemoUserSeedService demoUserSeedService;

    public DemoUserSeedController(DemoUserSeedService demoUserSeedService) {
        this.demoUserSeedService = demoUserSeedService;
    }

    @Operation(
            summary = "Seed demo users for the organization",
            description = "Creates demo users for OWNER, ADMIN, COMPLIANCE_MANAGER, MEMBER, and AUDITOR. Enable with APP_DEMO_USERS_ENABLED=true.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/seed-demo-users")
    public ResponseEntity<SeedDemoUsersResponse> seedDemoUsers(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                demoUserSeedService.seedDemoUsers(organizationId, authenticatedUser.id())
        );
    }
}
