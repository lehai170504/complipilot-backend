package com.complipilot.backend.organization.service;

import java.util.UUID;

import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.organization.dto.OrganizationSettingsResponse;
import com.complipilot.backend.organization.dto.UpdateOrganizationSettingsRequest;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.repository.OrganizationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationSettingsService {

    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;

    public OrganizationSettingsService(
            OrganizationRepository organizationRepository,
            TenantAccessService tenantAccessService
    ) {
        this.organizationRepository = organizationRepository;
        this.tenantAccessService = tenantAccessService;
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