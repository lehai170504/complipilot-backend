package com.complipilot.backend.evidence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.complipilot.backend.evidence.entity.EvidenceDocument;
import com.complipilot.backend.evidence.enums.EvidenceSourceType;
import com.complipilot.backend.evidence.enums.EvidenceStatus;
import com.complipilot.backend.evidence.enums.EvidenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
        SELECT evidence
        FROM EvidenceDocument evidence
        WHERE evidence.organization.id = :organizationId
          AND evidence.status <> :archivedStatus
          AND (:evidenceType IS NULL OR evidence.evidenceType = :evidenceType)
          AND (:sourceType IS NULL OR evidence.sourceType = :sourceType)
        """)
    Page<EvidenceDocument> findByOrganizationIdWithFilters(
            @Param("organizationId") UUID organizationId,
            @Param("archivedStatus") EvidenceStatus archivedStatus,
            @Param("evidenceType") EvidenceType evidenceType,
            @Param("sourceType") EvidenceSourceType sourceType,
            Pageable pageable
    );

    @Query("""
        SELECT evidence
        FROM EvidenceDocument evidence
        WHERE evidence.organization.id = :organizationId
          AND evidence.status <> :archivedStatus
          AND (:evidenceType IS NULL OR evidence.evidenceType = :evidenceType)
          AND (:sourceType IS NULL OR evidence.sourceType = :sourceType)
          AND (
                :query IS NULL
                OR LOWER(evidence.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(evidence.description) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(evidence.externalUrl) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        """)
    Page<EvidenceDocument> findByOrganizationIdWithFilters(
            @Param("organizationId") UUID organizationId,
            @Param("archivedStatus") EvidenceStatus archivedStatus,
            @Param("evidenceType") EvidenceType evidenceType,
            @Param("sourceType") EvidenceSourceType sourceType,
            @Param("query") String query,
            Pageable pageable
    );
}