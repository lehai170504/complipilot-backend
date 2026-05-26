package com.complipilot.backend.identity.dto;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        String fullName
) {
}