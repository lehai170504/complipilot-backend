package com.complipilot.backend.identity.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        AuthUserResponse user
) {
}