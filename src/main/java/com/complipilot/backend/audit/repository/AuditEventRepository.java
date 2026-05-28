package com.complipilot.backend.audit.repository;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.audit.entity.AuditEvent;
import com.complipilot.backend.audit.enums.AuditAction;
import com.complipilot.backend.audit.enums.AuditResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findTop50ByOrganization_IdOrderByCreatedAtDesc(UUID organizationId);

    Page<AuditEvent> findByOrganization_Id(UUID organizationId, Pageable pageable);

    @Query("""
        SELECT event
        FROM AuditEvent event
        WHERE event.organization.id = :organizationId
          AND (:action IS NULL OR event.action = :action)
          AND (:resourceType IS NULL OR event.resourceType = :resourceType)
        """)
    Page<AuditEvent> findByOrganizationIdWithFilters(
            @Param("organizationId") UUID organizationId,
            @Param("action") AuditAction action,
            @Param("resourceType") AuditResourceType resourceType,
            Pageable pageable
    );
}