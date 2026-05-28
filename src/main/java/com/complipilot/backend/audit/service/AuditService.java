package com.complipilot.backend.audit.service;

import com.complipilot.backend.audit.dto.AuditEventResponse;
import com.complipilot.backend.audit.entity.AuditEvent;
import com.complipilot.backend.audit.enums.AuditAction;
import com.complipilot.backend.audit.enums.AuditResourceType;
import com.complipilot.backend.audit.repository.AuditEventRepository;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.common.pagination.PageResponse;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.repository.OrganizationRepository;
import com.complipilot.backend.organization.service.TenantAccessService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TenantAccessService tenantAccessService;

    public AuditService(
            AuditEventRepository auditEventRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            TenantAccessService tenantAccessService
    ) {
        this.auditEventRepository = auditEventRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.tenantAccessService = tenantAccessService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            UUID organizationId,
            UUID actorUserId,
            AuditAction action,
            AuditResourceType resourceType,
            UUID resourceId,
            String summary,
            String metadataJson
    ) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        User actorUser = actorUserId == null
                ? null
                : userRepository.findById(actorUserId)
                .orElseThrow(() -> new NotFoundException("Actor user not found"));

        auditEventRepository.save(
                new AuditEvent(
                        organization,
                        actorUser,
                        action,
                        resourceType,
                        resourceId,
                        summary,
                        metadataJson
                )
        );
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> listRecentEvents(
            UUID organizationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        return auditEventRepository
                .findTop50ByOrganization_IdOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> listAuditEventsPage(
            UUID organizationId,
            UUID currentUserId,
            int page,
            int size
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        return PageResponse.from(
                auditEventRepository
                        .findByOrganization_Id(
                                organizationId,
                                PageRequest.of(
                                        safePage,
                                        safeSize,
                                        Sort.by(Sort.Direction.DESC, "createdAt")
                                )
                        )
                        .map(this::toResponse)
        );
    }

    private AuditEventResponse toResponse(AuditEvent auditEvent) {
        UUID actorUserId = auditEvent.getActorUser() == null
                ? null
                : auditEvent.getActorUser().getId();

        String actorEmail = auditEvent.getActorUser() == null
                ? null
                : auditEvent.getActorUser().getEmail();

        return new AuditEventResponse(
                auditEvent.getId(),
                auditEvent.getOrganization().getId(),
                actorUserId,
                actorEmail,
                auditEvent.getAction(),
                auditEvent.getResourceType(),
                auditEvent.getResourceId(),
                auditEvent.getSummary(),
                auditEvent.getMetadataJson(),
                auditEvent.getCreatedAt()
        );
    }
}
