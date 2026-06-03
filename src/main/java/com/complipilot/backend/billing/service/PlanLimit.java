package com.complipilot.backend.billing.service;

public record PlanLimit(
        int maxMembers,
        int maxEvidenceDocuments,
        long maxStorageBytes,
        int maxAiAnalysesPerMonth
) {
}
