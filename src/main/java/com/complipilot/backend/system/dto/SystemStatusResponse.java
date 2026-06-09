package com.complipilot.backend.system.dto;

import java.time.Instant;
import java.util.List;

public record SystemStatusResponse(
        String status,
        String service,
        String version,
        Instant timestamp,
        List<SystemStatusComponentResponse> components
) {
}