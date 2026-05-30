package com.complipilot.backend.evidence.repository;

import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.evidence.entity.EvidenceAiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceAiAnalysisRepository extends JpaRepository<EvidenceAiAnalysis, UUID> {

    Optional<EvidenceAiAnalysis> findTopByOrganization_IdAndEvidenceDocument_IdOrderByAnalyzedAtDesc(
            UUID organizationId,
            UUID evidenceDocumentId
    );
}