package com.complipilot.backend.compliance.service;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.common.error.ConflictException;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.compliance.dto.complianceItem.CompanyComplianceItemResponse;
import com.complipilot.backend.compliance.dto.complianceItem.CreateCompanyComplianceItemRequest;
import com.complipilot.backend.compliance.dto.framework.CreateFrameworkRequest;
import com.complipilot.backend.compliance.dto.requirement.CreateRequirementRequest;
import com.complipilot.backend.compliance.dto.framework.FrameworkResponse;
import com.complipilot.backend.compliance.dto.requirement.RequirementResponse;
import com.complipilot.backend.compliance.dto.complianceItem.UpdateCompanyComplianceItemRequest;
import com.complipilot.backend.compliance.entity.ComplianceFramework;
import com.complipilot.backend.compliance.entity.ComplianceRequirement;
import com.complipilot.backend.compliance.repository.CompanyComplianceItemRepository;
import com.complipilot.backend.compliance.repository.ComplianceFrameworkRepository;
import com.complipilot.backend.compliance.repository.ComplianceRequirementRepository;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.repository.OrganizationRepository;
import com.complipilot.backend.organization.service.TenantAccessService;
import com.complipilot.backend.organization.service.TenantAccessService;
import com.complipilot.backend.compliance.entity.CompanyComplianceItem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplianceService {

    private final ComplianceFrameworkRepository frameworkRepository;
    private final ComplianceRequirementRepository requirementRepository;
    private final CompanyComplianceItemRepository companyComplianceItemRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TenantAccessService tenantAccessService;

    public ComplianceService(
            ComplianceFrameworkRepository frameworkRepository,
            ComplianceRequirementRepository requirementRepository,
            CompanyComplianceItemRepository companyComplianceItemRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            TenantAccessService tenantAccessService
    ) {
        this.frameworkRepository = frameworkRepository;
        this.requirementRepository = requirementRepository;
        this.companyComplianceItemRepository = companyComplianceItemRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.tenantAccessService = tenantAccessService;
    }

    @Transactional
    public FrameworkResponse createFramework(CreateFrameworkRequest request) {
        String normalizedCode = request.code().trim().toUpperCase();

        if (frameworkRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new ConflictException("Compliance framework code already exists");
        }

        ComplianceFramework framework = frameworkRepository.save(
                new ComplianceFramework(
                        normalizedCode,
                        request.name(),
                        request.description(),
                        true
                )
        );

        return toFrameworkResponse(framework);
    }

    @Transactional(readOnly = true)
    public List<FrameworkResponse> listFrameworks() {
        return frameworkRepository.findAll()
                .stream()
                .map(this::toFrameworkResponse)
                .toList();
    }

    @Transactional
    public RequirementResponse createRequirement(
            UUID frameworkId,
            CreateRequirementRequest request
    ) {
        ComplianceFramework framework = frameworkRepository.findById(frameworkId)
                .orElseThrow(() -> new NotFoundException("Compliance framework not found"));

        ComplianceRequirement requirement = requirementRepository.save(
                new ComplianceRequirement(
                        framework,
                        request.code(),
                        request.title(),
                        request.description(),
                        request.category(),
                        request.sortOrder()
                )
        );

        return toRequirementResponse(requirement);
    }

    @Transactional(readOnly = true)
    public List<RequirementResponse> listRequirements(UUID frameworkId) {
        if (!frameworkRepository.existsById(frameworkId)) {
            throw new NotFoundException("Compliance framework not found");
        }

        return requirementRepository.findByFramework_IdOrderBySortOrderAsc(frameworkId)
                .stream()
                .map(this::toRequirementResponse)
                .toList();
    }

    @Transactional
    public CompanyComplianceItemResponse createCompanyComplianceItem(
            UUID organizationId,
            UUID currentUserId,
            CreateCompanyComplianceItemRequest request
    ) {
        tenantAccessService.requireManagerRole(organizationId, currentUserId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        ComplianceRequirement requirement = requirementRepository.findById(request.requirementId())
                .orElseThrow(() -> new NotFoundException("Compliance requirement not found"));

        if (companyComplianceItemRepository.existsByOrganization_IdAndRequirement_Id(
                organizationId,
                request.requirementId()
        )) {
            throw new ConflictException("Compliance item already exists for this organization and requirement");
        }

        CompanyComplianceItem item = companyComplianceItemRepository.save(
                new CompanyComplianceItem(
                        organization,
                        requirement
                )
        );

        return toCompanyComplianceItemResponse(item);
    }

    @Transactional(readOnly = true)
    public List<CompanyComplianceItemResponse> listCompanyComplianceItems(
            UUID organizationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        return companyComplianceItemRepository.findByOrganization_Id(organizationId)
                .stream()
                .map(this::toCompanyComplianceItemResponse)
                .toList();
    }

    @Transactional
    public CompanyComplianceItemResponse updateCompanyComplianceItem(
            UUID organizationId,
            UUID itemId,
            UUID currentUserId,
            UpdateCompanyComplianceItemRequest request
    ) {
        tenantAccessService.requireManagerRole(organizationId, currentUserId);

        CompanyComplianceItem item = companyComplianceItemRepository
                .findByIdAndOrganization_Id(itemId, organizationId)
                .orElseThrow(() -> new NotFoundException("Compliance item not found"));

        if (request.status() != null) {
            item.updateStatus(request.status());
        }

        User ownerUser = null;
        if (request.ownerUserId() != null) {
            tenantAccessService.requireActiveMember(organizationId, request.ownerUserId());

            ownerUser = userRepository.findById(request.ownerUserId())
                    .orElseThrow(() -> new NotFoundException("Owner user not found"));
        }

        item.updateDetails(
                ownerUser,
                request.dueDate(),
                request.notes()
        );

        return toCompanyComplianceItemResponse(item);
    }

    private FrameworkResponse toFrameworkResponse(ComplianceFramework framework) {
        return new FrameworkResponse(
                framework.getId(),
                framework.getCode(),
                framework.getName(),
                framework.getDescription(),
                framework.isSystemTemplate()
        );
    }

    private RequirementResponse toRequirementResponse(ComplianceRequirement requirement) {
        return new RequirementResponse(
                requirement.getId(),
                requirement.getFramework().getId(),
                requirement.getCode(),
                requirement.getTitle(),
                requirement.getDescription(),
                requirement.getCategory(),
                requirement.getSortOrder()
        );
    }

    private CompanyComplianceItemResponse toCompanyComplianceItemResponse(
            CompanyComplianceItem item
    ) {
        UUID ownerUserId = item.getOwnerUser() == null
                ? null
                : item.getOwnerUser().getId();

        return new CompanyComplianceItemResponse(
                item.getId(),
                item.getOrganization().getId(),
                item.getRequirement().getId(),
                item.getRequirement().getCode(),
                item.getRequirement().getTitle(),
                item.getStatus(),
                ownerUserId,
                item.getDueDate(),
                item.getNotes()
        );
    }
}