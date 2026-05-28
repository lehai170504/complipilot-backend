package com.complipilot.backend.identity.dto.login;

import com.complipilot.backend.identity.dto.AuthUserResponse;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        AuthUserResponse user
) {
}