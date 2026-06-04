package com.complipilot.backend.export.controller;

import java.time.LocalDate;
import java.util.UUID;

import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.export.service.CsvExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Exports", description = "CSV exports for compliance, evidence, and audit data")
@RestController
public class CsvExportController {

    private final CsvExportService csvExportService;

    public CsvExportController(CsvExportService csvExportService) {
        this.csvExportService = csvExportService;
    }

    @Operation(
            summary = "Export compliance items as CSV",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/exports/compliance-items.csv")
    public ResponseEntity<byte[]> exportComplianceItemsCsv(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        byte[] csv = csvExportService.exportComplianceItemsCsv(
                organizationId,
                authenticatedUser.id()
        );

        return csvResponse(
                csv,
                "compliance-items-" + LocalDate.now() + ".csv"
        );
    }

    @Operation(
            summary = "Export evidence documents as CSV",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/exports/evidence.csv")
    public ResponseEntity<byte[]> exportEvidenceDocumentsCsv(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        byte[] csv = csvExportService.exportEvidenceDocumentsCsv(
                organizationId,
                authenticatedUser.id()
        );

        return csvResponse(
                csv,
                "evidence-" + LocalDate.now() + ".csv"
        );
    }

    @Operation(
            summary = "Export audit events as CSV",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/exports/audit-events.csv")
    public ResponseEntity<byte[]> exportAuditEventsCsv(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        byte[] csv = csvExportService.exportAuditEventsCsv(
                organizationId,
                authenticatedUser.id()
        );

        return csvResponse(
                csv,
                "audit-events-" + LocalDate.now() + ".csv"
        );
    }

    private ResponseEntity<byte[]> csvResponse(byte[] csv, String filename) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }
}