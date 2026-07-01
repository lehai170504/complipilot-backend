package com.complipilot.backend.search.controller;

import java.util.UUID;

import com.complipilot.backend.search.dto.GlobalSearchResultDto;
import com.complipilot.backend.search.service.GlobalSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    public GlobalSearchController(GlobalSearchService globalSearchService) {
        this.globalSearchService = globalSearchService;
    }

    @GetMapping
    @PreAuthorize("@securityCheckService.isOrganizationMember(#organizationId)")
    public ResponseEntity<GlobalSearchResultDto> search(
            @RequestParam UUID organizationId,
            @RequestParam String query) {
        GlobalSearchResultDto result = globalSearchService.search(organizationId, query);
        return ResponseEntity.ok(result);
    }
}
