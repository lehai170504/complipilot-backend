package com.complipilot.backend.evidence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.evidence.entity.EvidenceDocument;
import com.complipilot.backend.evidence.enums.EvidenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceDocumentRepository extends JpaRepository<EvidenceDocument, UUID> {

    List<EvidenceDocument> findByOrganization_IdAndStatusOrderByCreatedAtDesc(
            UUID organizationId,
            EvidenceStatus status
    );

    Page<EvidenceDocument> findByOrganization_IdAndStatusNot(
            UUID organizationId,
            EvidenceStatus status,
            Pageable pageable
    );

    Optional<EvidenceDocument> findByIdAndOrganization_Id(
            UUID id,
            UUID organizationId
    );
}