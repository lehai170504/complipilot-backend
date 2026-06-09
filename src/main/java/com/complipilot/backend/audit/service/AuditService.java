package com.complipilot.backend.audit.service;

import com.complipilot.backend.audit.dto.AuditEventFilterRequest;
import com.complipilot.backend.audit.dto.AuditEventResponse;
import com.complipilot.backend.audit.entity.AuditEvent;
import com.complipilot.backend.audit.enums.AuditAction;
import com.complipilot.backend.audit.enums.AuditResourceType;
import com.complipilot.backend.audit.repository.AuditEventRepository;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.common.pagination.PageResponse;
import com.complipilot.backend.common.sorting.SortRequest;
import com.complipilot.backend.common.sorting.SortUtils;
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
import java.util.Map;
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
            AuditEventFilterRequest filter,
            SortRequest sort,
            int page,
            int size
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String normalizedQuery = normalizeQuery(filter.query());
        var pageable = PageRequest.of(
                safePage,
                safeSize,
                SortUtils.toSort(
                        sort,
                        ALLOWED_AUDIT_SORT_FIELDS,
                        "createdAt"
                )
        );

        return PageResponse.from(
                (normalizedQuery == null
                        ? auditEventRepository.findByOrganizationIdWithFilters(
                                organizationId,
                                filter.action(),
                                filter.resourceType(),
                                pageable
                        )
                        : auditEventRepository.findByOrganizationIdWithFilters(
                                organizationId,
                                filter.action(),
                                filter.resourceType(),
                                normalizedQuery,
                                pageable
                        ))
                        .map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> listCurrentUserActivity(
            UUID currentUserId,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        return PageResponse.from(
                auditEventRepository
                        .findByActorUser_IdOrderByCreatedAtDesc(
                                currentUserId,
                                PageRequest.of(safePage, safeSize)
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

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        return query.trim();
    }

    private static final Map<String, String> ALLOWED_AUDIT_SORT_FIELDS = Map.of(
            "createdAt", "createdAt",
            "action", "action",
            "resourceType", "resourceType",
            "actorEmail", "actorEmail"
    );

}
