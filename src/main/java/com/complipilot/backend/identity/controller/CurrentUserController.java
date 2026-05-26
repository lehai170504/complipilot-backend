package com.complipilot.backend.identity.controller;

import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.identity.dto.AuthUserResponse;

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
}