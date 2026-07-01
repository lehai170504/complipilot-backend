package com.complipilot.backend.search.dto;

import java.util.List;

public record GlobalSearchResultDto(
        List<SearchResultItemDto> complianceItems,
        List<SearchResultItemDto> evidence,
        List<SearchResultItemDto> tasks,
        List<SearchResultItemDto> auditEvents) {
}
