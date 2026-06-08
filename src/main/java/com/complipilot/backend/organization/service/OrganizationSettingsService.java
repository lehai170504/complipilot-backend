package com.complipilot.backend.organization.service;

import java.util.UUID;

import com.complipilot.backend.audit.enums.AuditAction;
import com.complipilot.backend.audit.enums.AuditResourceType;
import com.complipilot.backend.audit.service.AuditService;
import com.complipilot.backend.common.error.BadRequestException;
import com.complipilot.backend.common.error.ConflictException;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.organization.dto.DisableOrganizationRequest;
import com.complipilot.backend.organization.dto.OrganizationSettingsResponse;
import com.complipilot.backend.organization.dto.UpdateOrganizationSettingsRequest;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.enums.OrganizationStatus;
import com.complipilot.backend.organization.repository.OrganizationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationSettingsService {

    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;

    public OrganizationSettingsService(
            OrganizationRepository organizationRepository,
            TenantAccessService tenantAccessService,
            AuditService auditService
    ) {
        this.organizationRepository = organizationRepository;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public OrganizationSettingsResponse getSettings(
            UUID organizationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        return toResponse(organization);
    }

    @Transactional
    public OrganizationSettingsResponse updateSettings(
            UUID organizationId,
            UUID currentUserId,
            UpdateOrganizationSettingsRequest request
    ) {
        tenantAccessService.requireManagerRole(organizationId, currentUserId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        organization.rename(request.name());

        return toResponse(organization);
    }

    @Transactional
    public OrganizationSettingsResponse disableWorkspace(
            UUID organizationId,
            UUID currentUserId,
            DisableOrganizationRequest request
    ) {
        tenantAccessService.requireOwnerRole(organizationId, currentUserId);

        if (!"DISABLE".equals(request.confirmation())) {
            throw new BadRequestException("Confirmation must be DISABLE");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        if (organization.getStatus() == OrganizationStatus.DISABLED) {
            throw new ConflictException("Organization is already disabled");
        }

        organization.disable();

        auditService.record(
                organizationId,
                currentUserId,
                AuditAction.ORGANIZATION_DISABLED,
                AuditResourceType.ORGANIZATION,
                organizationId,
                "Workspace disabled: " + organization.getName(),
                "{\"confirmation\":\"DISABLE\"}"
        );

        return toResponse(organization);
    }

    private OrganizationSettingsResponse toResponse(Organization organization) {
        return new OrganizationSettingsResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getStatus(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
    }
}