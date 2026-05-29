package com.complipilot.backend.compliance.service;

import java.time.LocalDate;
import java.util.*;

import com.complipilot.backend.audit.enums.AuditAction;
import com.complipilot.backend.audit.enums.AuditResourceType;
import com.complipilot.backend.audit.service.AuditService;
import com.complipilot.backend.common.error.ConflictException;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.compliance.dto.ComplianceSummaryResponse;
import com.complipilot.backend.compliance.dto.framework.ApplyFrameworkResponse;
import com.complipilot.backend.compliance.dto.complianceItem.CompanyComplianceItemResponse;
import com.complipilot.backend.compliance.dto.complianceItem.CreateCompanyComplianceItemRequest;
import com.complipilot.backend.compliance.dto.framework.CreateFrameworkRequest;
import com.complipilot.backend.compliance.dto.requirement.CreateRequirementRequest;
import com.complipilot.backend.compliance.dto.framework.FrameworkResponse;
import com.complipilot.backend.compliance.dto.requirement.RequirementResponse;
import com.complipilot.backend.compliance.dto.complianceItem.UpdateCompanyComplianceItemRequest;
import com.complipilot.backend.compliance.entity.ComplianceFramework;
import com.complipilot.backend.compliance.entity.ComplianceFrameworkTranslation;
import com.complipilot.backend.compliance.entity.ComplianceRequirement;
import com.complipilot.backend.compliance.entity.ComplianceRequirementTranslation;
import com.complipilot.backend.compliance.enums.CompanyComplianceStatus;
import com.complipilot.backend.compliance.repository.CompanyComplianceItemRepository;
import com.complipilot.backend.compliance.repository.ComplianceFrameworkRepository;
import com.complipilot.backend.compliance.repository.ComplianceFrameworkTranslationRepository;
import com.complipilot.backend.compliance.repository.ComplianceRequirementRepository;
import com.complipilot.backend.compliance.repository.ComplianceRequirementTranslationRepository;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.repository.OrganizationRepository;
import com.complipilot.backend.organization.service.TenantAccessService;
import com.complipilot.backend.compliance.entity.CompanyComplianceItem;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplianceService {

    private final ComplianceFrameworkRepository frameworkRepository;
    private final ComplianceRequirementRepository requirementRepository;
    private final ComplianceFrameworkTranslationRepository frameworkTranslationRepository;
    private final ComplianceRequirementTranslationRepository requirementTranslationRepository;
    private final CompanyComplianceItemRepository companyComplianceItemRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;

    private static final int DUE_SOON_DAYS = 14;
    private static final String DEFAULT_LOCALE = "en";

    private static final Set<CompanyComplianceStatus> DONE_STATUSES = EnumSet.of(
            CompanyComplianceStatus.COMPLIANT,
            CompanyComplianceStatus.WAIVED
    );

    public ComplianceService(
            ComplianceFrameworkRepository frameworkRepository,
            ComplianceRequirementRepository requirementRepository,
            ComplianceFrameworkTranslationRepository frameworkTranslationRepository,
            ComplianceRequirementTranslationRepository requirementTranslationRepository,
            CompanyComplianceItemRepository companyComplianceItemRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            TenantAccessService tenantAccessService,
            AuditService auditService
    ) {
        this.frameworkRepository = frameworkRepository;
        this.requirementRepository = requirementRepository;
        this.frameworkTranslationRepository = frameworkTranslationRepository;
        this.requirementTranslationRepository = requirementTranslationRepository;
        this.companyComplianceItemRepository = companyComplianceItemRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
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

        upsertFrameworkTranslation(
                framework,
                DEFAULT_LOCALE,
                request.name(),
                request.description()
        );

        return toFrameworkResponse(framework);
    }

    @Transactional(readOnly = true)
    public List<FrameworkResponse> listFrameworks() {
        String locale = currentResponseLocale();

        return frameworkRepository.findAll()
                .stream()
                .map(framework -> toFrameworkResponse(framework, locale))
                .toList();
    }

    @Transactional
    public FrameworkResponse seedSecurityBaselineTemplate() {
        String frameworkCode = "SME-SECURITY-BASELINE";

        ComplianceFramework framework = frameworkRepository.findByCodeIgnoreCase(frameworkCode)
                .orElseGet(() -> frameworkRepository.save(
                        new ComplianceFramework(
                                frameworkCode,
                                "SME Security Baseline",
                                "A practical security baseline framework for small and medium-sized organizations.",
                                true
                        )
                ));

        upsertFrameworkTranslation(
                framework,
                DEFAULT_LOCALE,
                "SME Security Baseline",
                "A practical security baseline framework for small and medium-sized organizations."
        );
        upsertFrameworkTranslation(
                framework,
                "vi",
                "Baseline bảo mật cho SME",
                "Framework baseline bảo mật thực tế dành cho tổ chức vừa và nhỏ."
        );

        seedRequirementWithTranslations(
                framework,
                "SEC-001",
                "Enable multi-factor authentication",
                "All privileged and administrator accounts should use multi-factor authentication.",
                "Access Control",
                "Bật xác thực đa yếu tố",
                "Tất cả tài khoản đặc quyền và quản trị viên nên sử dụng xác thực đa yếu tố.",
                "Kiểm soát truy cập",
                1
        );

        seedRequirementWithTranslations(
                framework,
                "SEC-002",
                "Maintain user access review",
                "User access should be reviewed periodically to remove inactive or unnecessary access.",
                "Access Control",
                "Duy trì rà soát quyền truy cập người dùng",
                "Quyền truy cập của người dùng nên được rà soát định kỳ để loại bỏ truy cập không còn hoạt động hoặc không cần thiết.",
                "Kiểm soát truy cập",
                2
        );

        seedRequirementWithTranslations(
                framework,
                "SEC-003",
                "Keep evidence for critical controls",
                "Evidence documents should be collected and retained for key compliance controls.",
                "Evidence Management",
                "Lưu giữ bằng chứng cho các control quan trọng",
                "Tài liệu bằng chứng nên được thu thập và lưu giữ cho các control tuân thủ trọng yếu.",
                "Quản lý bằng chứng",
                3
        );

        seedRequirementWithTranslations(
                framework,
                "SEC-004",
                "Define incident response contact",
                "The organization should define responsible contacts for security incidents.",
                "Incident Response",
                "Xác định đầu mối ứng phó sự cố",
                "Tổ chức nên xác định người phụ trách liên hệ khi xảy ra sự cố bảo mật.",
                "Ứng phó sự cố",
                4
        );

        seedRequirementWithTranslations(
                framework,
                "SEC-005",
                "Backup critical business data",
                "Critical business data should be backed up and recoverable.",
                "Business Continuity",
                "Sao lưu dữ liệu kinh doanh quan trọng",
                "Dữ liệu kinh doanh quan trọng nên được sao lưu và có khả năng khôi phục.",
                "Liên tục kinh doanh",
                5
        );

        return toFrameworkResponse(framework);
    }

    private void seedRequirementWithTranslations(
            ComplianceFramework framework,
            String code,
            String titleEn,
            String descriptionEn,
            String categoryEn,
            String titleVi,
            String descriptionVi,
            String categoryVi,
            int sortOrder
    ) {
        ComplianceRequirement requirement = requirementRepository
                .findByFramework_IdOrderBySortOrderAsc(framework.getId())
                .stream()
                .filter(item -> item.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElseGet(() -> requirementRepository.save(
                        new ComplianceRequirement(
                                framework,
                                code,
                                titleEn,
                                descriptionEn,
                                categoryEn,
                                sortOrder
                        )
                ));

        upsertRequirementTranslation(
                requirement,
                DEFAULT_LOCALE,
                titleEn,
                descriptionEn,
                categoryEn
        );
        upsertRequirementTranslation(
                requirement,
                "vi",
                titleVi,
                descriptionVi,
                categoryVi
        );
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

        upsertRequirementTranslation(
                requirement,
                DEFAULT_LOCALE,
                request.title(),
                request.description(),
                request.category()
        );

        return toRequirementResponse(requirement);
    }

    @Transactional(readOnly = true)
    public List<RequirementResponse> listRequirements(UUID frameworkId) {
        if (!frameworkRepository.existsById(frameworkId)) {
            throw new NotFoundException("Compliance framework not found");
        }

        String locale = currentResponseLocale();

        return requirementRepository.findByFramework_IdOrderBySortOrderAsc(frameworkId)
                .stream()
                .map(requirement -> toRequirementResponse(requirement, locale))
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

        auditService.record(
                organizationId,
                currentUserId,
                AuditAction.COMPLIANCE_ITEM_CREATED,
                AuditResourceType.COMPLIANCE_ITEM,
                item.getId(),
                "Created compliance item",
                """
                {"requirementId":"%s","requirementCode":"%s"}
                """.formatted(requirement.getId(), requirement.getCode())
        );

        return toCompanyComplianceItemResponse(item);
    }

    @Transactional
    public ApplyFrameworkResponse applyFrameworkToOrganization(
            UUID organizationId,
            UUID frameworkId,
            UUID currentUserId
    ) {
        tenantAccessService.requireManagerRole(organizationId, currentUserId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        if (!frameworkRepository.existsById(frameworkId)) {
            throw new NotFoundException("Compliance framework not found");
        }

        List<ComplianceRequirement> requirements =
                requirementRepository.findByFramework_IdOrderBySortOrderAsc(frameworkId);

        List<CompanyComplianceItemResponse> createdItems = new ArrayList<>();
        int skippedCount = 0;

        for (ComplianceRequirement requirement : requirements) {
            boolean alreadyExists = companyComplianceItemRepository
                    .existsByOrganization_IdAndRequirement_Id(
                            organizationId,
                            requirement.getId()
                    );

            if (alreadyExists) {
                skippedCount++;
                continue;
            }

            CompanyComplianceItem item = companyComplianceItemRepository.save(
                    new CompanyComplianceItem(
                            organization,
                            requirement
                    )
            );

            createdItems.add(toCompanyComplianceItemResponse(item));
        }

        auditService.record(
                organizationId,
                currentUserId,
                AuditAction.COMPLIANCE_FRAMEWORK_APPLIED,
                AuditResourceType.COMPLIANCE_FRAMEWORK,
                frameworkId,
                "Applied compliance framework to organization",
                """
                {"createdCount":%d,"skippedCount":%d}
                """.formatted(createdItems.size(), skippedCount)
        );

        return new ApplyFrameworkResponse(
                organizationId,
                frameworkId,
                createdItems.size(),
                skippedCount,
                createdItems
        );
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

    @Transactional(readOnly = true)
    public ComplianceSummaryResponse getComplianceSummary(
            UUID organizationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        long totalItems = companyComplianceItemRepository.countByOrganization_Id(organizationId);

        return new ComplianceSummaryResponse(
                organizationId,
                totalItems,
                companyComplianceItemRepository.countByOrganization_IdAndStatus(
                        organizationId,
                        CompanyComplianceStatus.OPEN
                ),
                companyComplianceItemRepository.countByOrganization_IdAndStatus(
                        organizationId,
                        CompanyComplianceStatus.IN_PROGRESS
                ),
                companyComplianceItemRepository.countByOrganization_IdAndStatus(
                        organizationId,
                        CompanyComplianceStatus.READY_FOR_REVIEW
                ),
                companyComplianceItemRepository.countByOrganization_IdAndStatus(
                        organizationId,
                        CompanyComplianceStatus.COMPLIANT
                ),
                companyComplianceItemRepository.countByOrganization_IdAndStatus(
                        organizationId,
                        CompanyComplianceStatus.NON_COMPLIANT
                ),
                companyComplianceItemRepository.countByOrganization_IdAndStatus(
                        organizationId,
                        CompanyComplianceStatus.WAIVED
                )
        );
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

        CompanyComplianceStatus oldStatus = item.getStatus();

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

        auditService.record(
                organizationId,
                currentUserId,
                AuditAction.COMPLIANCE_ITEM_UPDATED,
                AuditResourceType.COMPLIANCE_ITEM,
                item.getId(),
                "Updated compliance item",
                """
                {"oldStatus":"%s","newStatus":"%s"}
                """.formatted(oldStatus, item.getStatus())
        );

        return toCompanyComplianceItemResponse(item);
    }

    @Transactional(readOnly = true)
    public List<CompanyComplianceItemResponse> listDueSoonComplianceItems(
            UUID organizationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        LocalDate today = LocalDate.now();
        LocalDate dueSoonUntil = today.plusDays(DUE_SOON_DAYS);

        return companyComplianceItemRepository
                .findByOrganization_IdAndDueDateBetweenAndStatusNotInOrderByDueDateAsc(
                        organizationId,
                        today,
                        dueSoonUntil,
                        DONE_STATUSES
                )
                .stream()
                .map(this::toCompanyComplianceItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyComplianceItemResponse> listOverdueComplianceItems(
            UUID organizationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        LocalDate today = LocalDate.now();

        return companyComplianceItemRepository
                .findByOrganization_IdAndDueDateBeforeAndStatusNotInOrderByDueDateAsc(
                        organizationId,
                        today,
                        DONE_STATUSES
                )
                .stream()
                .map(this::toCompanyComplianceItemResponse)
                .toList();
    }

    private FrameworkResponse toFrameworkResponse(ComplianceFramework framework) {
        return toFrameworkResponse(framework, currentResponseLocale());
    }

    private FrameworkResponse toFrameworkResponse(
            ComplianceFramework framework,
            String locale
    ) {
        Optional<ComplianceFrameworkTranslation> translation =
                findFrameworkTranslation(framework.getId(), locale);

        return new FrameworkResponse(
                framework.getId(),
                framework.getCode(),
                translation.map(ComplianceFrameworkTranslation::getName)
                        .orElse(framework.getName()),
                translation.map(ComplianceFrameworkTranslation::getDescription)
                        .orElse(framework.getDescription()),
                framework.isSystemTemplate()
        );
    }

    private RequirementResponse toRequirementResponse(ComplianceRequirement requirement) {
        return toRequirementResponse(requirement, currentResponseLocale());
    }

    private RequirementResponse toRequirementResponse(
            ComplianceRequirement requirement,
            String locale
    ) {
        Optional<ComplianceRequirementTranslation> translation =
                findRequirementTranslation(requirement.getId(), locale);

        return new RequirementResponse(
                requirement.getId(),
                requirement.getFramework().getId(),
                requirement.getCode(),
                translation.map(ComplianceRequirementTranslation::getTitle)
                        .orElse(requirement.getTitle()),
                translation.map(ComplianceRequirementTranslation::getDescription)
                        .orElse(requirement.getDescription()),
                translation.map(ComplianceRequirementTranslation::getCategory)
                        .orElse(requirement.getCategory()),
                requirement.getSortOrder()
        );
    }

    private CompanyComplianceItemResponse toCompanyComplianceItemResponse(
            CompanyComplianceItem item
    ) {
        UUID ownerUserId = item.getOwnerUser() == null
                ? null
                : item.getOwnerUser().getId();

        String locale = currentResponseLocale();
        ComplianceRequirement requirement = item.getRequirement();
        String requirementTitle = findRequirementTranslation(requirement.getId(), locale)
                .map(ComplianceRequirementTranslation::getTitle)
                .orElse(requirement.getTitle());

        return new CompanyComplianceItemResponse(
                item.getId(),
                item.getOrganization().getId(),
                requirement.getId(),
                requirement.getCode(),
                requirementTitle,
                item.getStatus(),
                ownerUserId,
                item.getDueDate(),
                item.getNotes()
        );
    }

    private Optional<ComplianceFrameworkTranslation> findFrameworkTranslation(
            UUID frameworkId,
            String locale
    ) {
        return frameworkTranslationRepository.findByFramework_IdAndLocale(
                        frameworkId,
                        locale
                )
                .or(() -> frameworkTranslationRepository.findByFramework_IdAndLocale(
                        frameworkId,
                        DEFAULT_LOCALE
                ));
    }

    private Optional<ComplianceRequirementTranslation> findRequirementTranslation(
            UUID requirementId,
            String locale
    ) {
        return requirementTranslationRepository.findByRequirement_IdAndLocale(
                        requirementId,
                        locale
                )
                .or(() -> requirementTranslationRepository.findByRequirement_IdAndLocale(
                        requirementId,
                        DEFAULT_LOCALE
                ));
    }

    private void upsertFrameworkTranslation(
            ComplianceFramework framework,
            String locale,
            String name,
            String description
    ) {
        frameworkTranslationRepository.findByFramework_IdAndLocale(
                        framework.getId(),
                        normalizeLocale(locale)
                )
                .ifPresentOrElse(
                        translation -> translation.updateText(name, description),
                        () -> frameworkTranslationRepository.save(
                                new ComplianceFrameworkTranslation(
                                        framework,
                                        normalizeLocale(locale),
                                        name,
                                        description
                                )
                        )
                );
    }

    private void upsertRequirementTranslation(
            ComplianceRequirement requirement,
            String locale,
            String title,
            String description,
            String category
    ) {
        requirementTranslationRepository.findByRequirement_IdAndLocale(
                        requirement.getId(),
                        normalizeLocale(locale)
                )
                .ifPresentOrElse(
                        translation -> translation.updateText(title, description, category),
                        () -> requirementTranslationRepository.save(
                                new ComplianceRequirementTranslation(
                                        requirement,
                                        normalizeLocale(locale),
                                        title,
                                        description,
                                        category
                                )
                        )
                );
    }

    private String currentResponseLocale() {
        String language = LocaleContextHolder.getLocale().getLanguage();

        return normalizeLocale(language);
    }

    private String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return DEFAULT_LOCALE;
        }

        String normalized = locale.trim().toLowerCase(Locale.ROOT);

        if (normalized.startsWith("vi")) {
            return "vi";
        }

        return DEFAULT_LOCALE;
    }
}