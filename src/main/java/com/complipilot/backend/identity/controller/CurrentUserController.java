package com.complipilot.backend.identity;

import java.util.List;

import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.identity.dto.AuthUserResponse;
import com.complipilot.backend.organization.service.OrganizationMembershipService;
import com.complipilot.backend.organization.dto.OrganizationMembershipResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Current User", description = "Current authenticated user APIs")
@RestController
public class CurrentUserController {

    private final OrganizationMembershipService organizationMembershipService;

    public CurrentUserController(
            OrganizationMembershipService organizationMembershipService
    ) {
        this.organizationMembershipService = organizationMembershipService;
    }

    @Operation(
            summary = "Get current user",
            description = "Returns the currently authenticated user from the JWT access token.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/me")
    public ResponseEntity<AuthUserResponse> currentUser(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                new AuthUserResponse(
                        authenticatedUser.id(),
                        authenticatedUser.email(),
                        authenticatedUser.fullName()
                )
        );
    }

    @Operation(
            summary = "Get current user's organizations",
            description = "Returns active organization memberships for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/me/organizations")
    public ResponseEntity<List<OrganizationMembershipResponse>> currentUserOrganizations(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                organizationMembershipService.findActiveMemberships(authenticatedUser.id())
        );
    }
}