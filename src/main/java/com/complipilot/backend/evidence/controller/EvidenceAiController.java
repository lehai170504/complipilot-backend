package com.complipilot.backend.evidence.controller;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.common.security.AuthenticatedUser;
import com.complipilot.backend.evidence.dto.EvidenceAiAnalysisRecordResponse;
import com.complipilot.backend.evidence.service.EvidenceAiAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Evidence AI", description = "AI-assisted evidence analysis")
@RestController
public class EvidenceAiController {

    private final EvidenceAiAnalysisService evidenceAiAnalysisService;

    public EvidenceAiController(
            EvidenceAiAnalysisService evidenceAiAnalysisService
    ) {
        this.evidenceAiAnalysisService = evidenceAiAnalysisService;
    }

    @Operation(
            summary = "Analyze evidence with AI",
            description = "Runs AI analysis for an evidence document, saves the result, and returns the saved analysis.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/organizations/{organizationId}/evidence/{evidenceId}/ai/analyze")
    public EvidenceAiAnalysisRecordResponse analyzeEvidence(
            @PathVariable UUID organizationId,
            @PathVariable UUID evidenceId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return evidenceAiAnalysisService.analyzeAndSave(
                organizationId,
                evidenceId,
                authenticatedUser.id()
        );
    }

    @Operation(
            summary = "Get latest evidence AI analysis",
            description = "Returns the latest saved AI analysis for an evidence document.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/evidence/{evidenceId}/ai/analysis/latest")
    public EvidenceAiAnalysisRecordResponse getLatestAnalysis(
            @PathVariable UUID organizationId,
            @PathVariable UUID evidenceId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return evidenceAiAnalysisService.getLatestAnalysis(
                organizationId,
                evidenceId,
                authenticatedUser.id()
        );
    }

    @Operation(
            summary = "List evidence AI analysis history",
            description = "Returns the latest saved AI analysis records for an evidence document.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/organizations/{organizationId}/evidence/{evidenceId}/ai/analyses")
    public List<EvidenceAiAnalysisRecordResponse> listAnalysisHistory(
            @PathVariable UUID organizationId,
            @PathVariable UUID evidenceId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return evidenceAiAnalysisService.listAnalysisHistory(
                organizationId,
                evidenceId,
                authenticatedUser.id()
        );
    }

}