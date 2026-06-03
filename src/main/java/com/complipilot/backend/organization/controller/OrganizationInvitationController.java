package com.complipilot.backend.organization.controller;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.organization.dto.AcceptOrganizationInvitationRequest;
import com.complipilot.backend.organization.dto.CreateOrganizationInvitationRequest;
import com.complipilot.backend.organization.dto.OrganizationInvitationResponse;
import com.complipilot.backend.organization.dto.OrganizationMemberResponse;
import com.complipilot.backend.organization.service.OrganizationInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Organization Invitations", description = "Organization member invitation APIs")
@RestController
public class OrganizationInvitationController {

    private final OrganizationInvitationService organizationInvitationService;

    public OrganizationInvitationController(
            OrganizationInvitationService organizationInvitationService
    ) {
        this.organizationInvitationService = organizationInvitationService;
    }

    @Operation(
            summary = "Create organization invitation",
            description = "Creates a pending invitation for an email address. OWNER and ADMIN members can invite users.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/organizations/{organizationId}/invitations")
    public ResponseEntity<OrganizationInvitationResponse> createInvitation(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateOrganizationInvitationRequest request
    ) {
        OrganizationInvitationResponse response = organizationInvitationService.createInvitation(
                organizationId,
                authenticatedUser.id(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List organization invitations",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/invitations")
    public ResponseEntity<List<OrganizationInvitationResponse>> listInvitations(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                organizationInvitationService.listInvitations(
                        organizationId,
                        authenticatedUser.id()
                )
        );
    }

    @Operation(
            summary = "Get invitation by token",
            description = "Returns public invitation metadata for the accept-invitation page."
    )
    @GetMapping("/api/v1/organization-invitations/{token}")
    public ResponseEntity<OrganizationInvitationResponse> getInvitationByToken(
            @PathVariable String token
    ) {
        return ResponseEntity.ok(
                organizationInvitationService.getInvitationByToken(token)
        );
    }

    @Operation(
            summary = "Accept organization invitation",
            description = "Accepts an invitation token and creates or reactivates an organization membership."
    )
    @PostMapping("/api/v1/organization-invitations/{token}/accept")
    public ResponseEntity<OrganizationMemberResponse> acceptInvitation(
            @PathVariable String token,
            @Valid @RequestBody AcceptOrganizationInvitationRequest request
    ) {
        OrganizationMemberResponse response = organizationInvitationService.acceptInvitation(
                token,
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Revoke organization invitation",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/api/v1/organizations/{organizationId}/invitations/{invitationId}")
    public ResponseEntity<Void> revokeInvitation(
            @PathVariable UUID organizationId,
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        organizationInvitationService.revokeInvitation(
                organizationId,
                invitationId,
                authenticatedUser.id()
        );

        return ResponseEntity.noContent().build();
    }
}
