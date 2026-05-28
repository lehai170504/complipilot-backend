package com.complipilot.backend.audit.repository;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.audit.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findTop50ByOrganization_IdOrderByCreatedAtDesc(UUID organizationId);

    Page<AuditEvent> findByOrganization_Id(UUID organizationId, Pageable pageable);
}