package com.complipilot.backend.system.controller;

import com.complipilot.backend.system.dto.SystemStatusResponse;
import com.complipilot.backend.system.service.SystemStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System", description = "System diagnostics and operational status")
@RestController
public class SystemStatusController {

    private final SystemStatusService systemStatusService;

    public SystemStatusController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    @Operation(
            summary = "Get system status",
            description = "Returns backend, database, storage, AI, and mail configuration status.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/system/status")
    public ResponseEntity<SystemStatusResponse> getSystemStatus() {
        return ResponseEntity.ok(systemStatusService.getStatus());
    }
}