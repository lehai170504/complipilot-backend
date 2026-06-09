package com.complipilot.backend.system.dto;

import java.util.Map;

public record SystemStatusComponentResponse(
        String key,
        String label,
        String status,
        String message,
        Map<String, Object> details
) {
}