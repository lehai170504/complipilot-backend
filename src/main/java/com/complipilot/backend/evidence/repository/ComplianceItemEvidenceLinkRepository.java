package com.complipilot.backend.evidence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.evidence.entity.ComplianceItemEvidenceLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceItemEvidenceLinkRepository extends JpaRepository<ComplianceItemEvidenceLink, UUID> {

    List<ComplianceItemEvidenceLink> findByComplianceItem_Id(UUID complianceItemId);

    Optional<ComplianceItemEvidenceLink> findByComplianceItem_IdAndEvidenceDocument_Id(
            UUID complianceItemId,
            UUID evidenceDocumentId
    );

    boolean existsByComplianceItem_IdAndEvidenceDocument_Id(
            UUID complianceItemId,
            UUID evidenceDocumentId
    );

    void deleteByComplianceItem_IdAndEvidenceDocument_Id(
            UUID complianceItemId,
            UUID evidenceDocumentId
    );
}