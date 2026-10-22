package com.complipilot.backend.search.controller;

import java.util.UUID;

import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.organization.service.TenantAccessService;
import com.complipilot.backend.search.dto.GlobalSearchResultDto;
import com.complipilot.backend.search.service.GlobalSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;
    private final TenantAccessService tenantAccessService;

    public GlobalSearchController(
            GlobalSearchService globalSearchService,
            TenantAccessService tenantAccessService
    ) {
        this.globalSearchService = globalSearchService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public ResponseEntity<GlobalSearchResultDto> search(
            @RequestParam UUID organizationId,
            @RequestParam String query,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        tenantAccessService.requireActiveMember(organizationId, authenticatedUser.id());
        GlobalSearchResultDto result = globalSearchService.search(organizationId, query);
        return ResponseEntity.ok(result);
    }
}
