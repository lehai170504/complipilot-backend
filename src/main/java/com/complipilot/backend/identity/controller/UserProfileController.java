package com.complipilot.backend.identity.controller;

import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.identity.dto.UpdateUserProfileRequest;
import com.complipilot.backend.identity.dto.UserProfileResponse;
import com.complipilot.backend.identity.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Profile", description = "Authenticated user profile APIs")
@RestController
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @Operation(
            summary = "Get current user profile",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                userProfileService.getProfile(authenticatedUser.id())
        );
    }

    @Operation(
            summary = "Update current user profile",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/api/v1/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ResponseEntity.ok(
                userProfileService.updateProfile(
                        authenticatedUser.id(),
                        request
                )
        );
    }
}