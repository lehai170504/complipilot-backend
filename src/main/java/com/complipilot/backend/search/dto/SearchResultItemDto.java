package com.complipilot.backend.search.dto;

import java.util.UUID;

public record SearchResultItemDto(
        UUID id,
        String title,
        String description,
        String url
) {
}
