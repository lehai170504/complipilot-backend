package com.complipilot.backend.identity.controller;

import com.complipilot.backend.identity.dto.login.LoginRequest;
import com.complipilot.backend.identity.dto.login.LoginResponse;
import com.complipilot.backend.identity.dto.register.RegisterRequest;
import com.complipilot.backend.identity.dto.register.RegisterResponse;

import com.complipilot.backend.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Register and login APIs")
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register a new user and organization",
            description = "Creates a user account, an organization workspace, and assigns the user as OWNER."
    )
    @PostMapping("/api/v1/auth/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Login with email and password",
            description = "Authenticates a user and returns a JWT access token."
    )
    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}