package com.complipilot.backend.organization.controller;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.organization.dto.CreateOrganizationMemberRequest;
import com.complipilot.backend.organization.dto.OrganizationMemberResponse;
import com.complipilot.backend.organization.dto.UpdateOrganizationMemberRoleRequest;
import com.complipilot.backend.organization.dto.UpdateOrganizationMemberStatusRequest;
import com.complipilot.backend.organization.service.OrganizationMemberManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Organization Members", description = "Organization member and role management APIs")
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/members")
public class OrganizationMemberController {

    private final OrganizationMemberManagementService organizationMemberManagementService;

    public OrganizationMemberController(
            OrganizationMemberManagementService organizationMemberManagementService
    ) {
        this.organizationMemberManagementService = organizationMemberManagementService;
    }

    @Operation(
            summary = "List organization members",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<List<OrganizationMemberResponse>> listMembers(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                organizationMemberManagementService.listMembers(
                        organizationId,
                        authenticatedUser.id()
                )
        );
    }

    @Operation(
            summary = "Create or invite an organization member",
            description = "OWNER and ADMIN members can create a user membership. If the email already exists, the existing user is added to the organization.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<OrganizationMemberResponse> createMember(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateOrganizationMemberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                organizationMemberManagementService.createMember(
                        organizationId,
                        authenticatedUser.id(),
                        request
                )
        );
    }

    @Operation(
            summary = "Update organization member role",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{memberId}/role")
    public ResponseEntity<OrganizationMemberResponse> updateRole(
            @PathVariable UUID organizationId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateOrganizationMemberRoleRequest request
    ) {
        return ResponseEntity.ok(
                organizationMemberManagementService.updateRole(
                        organizationId,
                        memberId,
                        authenticatedUser.id(),
                        request
                )
        );
    }

    @Operation(
            summary = "Update organization member status",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{memberId}/status")
    public ResponseEntity<OrganizationMemberResponse> updateStatus(
            @PathVariable UUID organizationId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateOrganizationMemberStatusRequest request
    ) {
        return ResponseEntity.ok(
                organizationMemberManagementService.updateStatus(
                        organizationId,
                        memberId,
                        authenticatedUser.id(),
                        request
                )
        );
    }

    @Operation(
            summary = "Disable an organization member",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID organizationId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        organizationMemberManagementService.removeMember(
                organizationId,
                memberId,
                authenticatedUser.id()
        );

        return ResponseEntity.noContent().build();
    }
}
