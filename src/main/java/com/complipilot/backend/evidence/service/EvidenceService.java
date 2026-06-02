package com.complipilot.backend.evidence.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.complipilot.backend.audit.enums.AuditAction;
import com.complipilot.backend.audit.enums.AuditResourceType;
import com.complipilot.backend.audit.service.AuditService;
import com.complipilot.backend.common.error.ConflictException;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.common.pagination.PageResponse;
import com.complipilot.backend.common.sorting.SortRequest;
import com.complipilot.backend.common.sorting.SortUtils;
import com.complipilot.backend.common.storage.supabase.SupabaseSignedDownloadResponse;
import com.complipilot.backend.common.storage.supabase.SupabaseSignedUploadResponse;
import com.complipilot.backend.common.storage.supabase.SupabaseStorageClient;
import com.complipilot.backend.compliance.entity.CompanyComplianceItem;
import com.complipilot.backend.compliance.repository.CompanyComplianceItemRepository;
import com.complipilot.backend.evidence.dto.*;
import com.complipilot.backend.evidence.entity.ComplianceItemEvidenceLink;
import com.complipilot.backend.evidence.entity.EvidenceDocument;
import com.complipilot.backend.evidence.enums.EvidenceSourceType;
import com.complipilot.backend.evidence.enums.EvidenceStatus;
import com.complipilot.backend.evidence.repository.ComplianceItemEvidenceLinkRepository;
import com.complipilot.backend.evidence.repository.EvidenceDocumentRepository;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.repository.OrganizationRepository;
import com.complipilot.backend.organization.service.TenantAccessService;
import com.complipilot.backend.common.storage.StorageProperties;
import com.complipilot.backend.common.storage.StorageService;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.complipilot.backend.common.storage.EvidenceObjectKeyService;
import org.springframework.beans.factory.ObjectProvider;

@Service
public class EvidenceService {

    private final EvidenceDocumentRepository evidenceDocumentRepository;
    private final ComplianceItemEvidenceLinkRepository evidenceLinkRepository;
    private final CompanyComplianceItemRepository complianceItemRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TenantAccessService tenantAccessService;
    private final ObjectProvider<StorageService> storageServiceProvider;
    private final EvidenceObjectKeyService evidenceObjectKeyService;
    private final StorageProperties storageProperties;
    private final AuditService auditService;
    private final SupabaseStorageClient supabaseStorageClient;

    public EvidenceService(
            EvidenceDocumentRepository evidenceDocumentRepository,
            ComplianceItemEvidenceLinkRepository evidenceLinkRepository,
            CompanyComplianceItemRepository complianceItemRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            TenantAccessService tenantAccessService,
            ObjectProvider<StorageService> storageServiceProvider,
            EvidenceObjectKeyService evidenceObjectKeyService,
            StorageProperties storageProperties,
            SupabaseStorageClient supabaseStorageClient,
            AuditService auditService
    ) {
        this.evidenceDocumentRepository = evidenceDocumentRepository;
        this.evidenceLinkRepository = evidenceLinkRepository;
        this.complianceItemRepository = complianceItemRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.tenantAccessService = tenantAccessService;
        this.storageServiceProvider = storageServiceProvider;
        this.evidenceObjectKeyService = evidenceObjectKeyService;
        this.storageProperties = storageProperties;
        this.supabaseStorageClient = supabaseStorageClient;
        this.auditService = auditService;
    }

    @Transactional
    public EvidenceDocumentResponse createEvidenceDocument(
            UUID organizationId,
            UUID currentUserId,
            CreateEvidenceDocumentRequest request
    ) {
        tenantAccessService.requireManagerRole(organizationId, currentUserId);

        validateEvidenceSource(request.sourceType(), request.fileObjectKey(), request.externalUrl());

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        User uploadedByUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        EvidenceDocument evidenceDocument = evidenceDocumentRepository.save(
                new EvidenceDocument(
                        organization,
                        request.title(),
                        request.description(),
                        request.evidenceType(),
                        request.sourceType(),
                        request.fileObjectKey(),
                        request.externalUrl(),
                        request.contentType(),
                        request.fileSizeBytes(),
                        uploadedByUser
                )
        );

        auditService.record(
                organizationId,
                currentUserId,
                AuditAction.EVIDENCE_DOCUMENT_CREATED,
                AuditResourceType.EVIDENCE_DOCUMENT,
                evidenceDocument.getId(),
                "Created evidence document",
                """
                {"title":"%s","sourceType":"%s","evidenceType":"%s"}
                """.formatted(
                        evidenceDocument.getTitle(),
                        evidenceDocument.getSourceType(),
                        evidenceDocument.getEvidenceType()
                )
        );

        return toEvidenceDocumentResponse(evidenceDocument);
    }

    @Transactional(readOnly = true)
    public PageResponse<EvidenceDocumentResponse> listEvidenceDocuments(
            UUID organizationId,
            UUID currentUserId,
            EvidenceFilterRequest filter,
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
                        ALLOWED_EVIDENCE_SORT_FIELDS,
                        "createdAt"
                )
        );

        return PageResponse.from(
                (normalizedQuery == null
                        ? evidenceDocumentRepository.findByOrganizationIdWithFilters(
                                organizationId,
                                EvidenceStatus.ARCHIVED,
                                filter.evidenceType(),
                                filter.sourceType(),
                                pageable
                        )
                        : evidenceDocumentRepository.findByOrganizationIdWithFilters(
                                organizationId,
                                EvidenceStatus.ARCHIVED,
                                filter.evidenceType(),
                                filter.sourceType(),
                                normalizedQuery,
                                pageable
                        ))
                        .map(this::toEvidenceDocumentResponse)
        );
    }

    @Transactional
    public EvidenceDocumentResponse updateEvidenceDocument(
            UUID organizationId,
            UUID evidenceId,
            UUID currentUserId,
            UpdateEvidenceDocumentRequest request
    ) {
        tenantAccessService.requireManagerRole(organizationId, currentUserId);

        EvidenceDocument evidenceDocument = evidenceDocumentRepository
                .findByIdAndOrganization_Id(evidenceId, organizationId)
                .orElseThrow(() -> new NotFoundException("Evidence document not found"));

        evidenceDocument.updateMetadata(
                request.title(),
                request.description(),
                request.evidenceType(),
                request.externalUrl()
        );

        auditService.record(
                organizationId,
                currentUserId,
                AuditAction.EVIDENCE_DOCUMENT_UPDATED,
                AuditResourceType.EVIDENCE_DOCUMENT,
                evidenceDocument.getId(),
                "Updated evidence document",
                """
                {"title":"%s","evidenceType":"%s"}
                """.formatted(
                        evidenceDocument.getTitle(),
                        evidenceDocument.getEvidenceType()
                )
        );

        return toEvidenceDocumentResponse(evidenceDocument);
    }

    @Transactional
    public void archiveEvidenceDocument(
            UUID organizationId,
            UUID evidenceId,
            UUID currentUserId
    ) {
        tenantAccessService.requireManagerRole(organizationId, currentUserId);

        EvidenceDocument evidenceDocument = evidenceDocumentRepository
                .findByIdAndOrganization_Id(evidenceId, organizationId)
                .orElseThrow(() -> new NotFoundException("Evidence document not found"));

        evidenceDocument.archive();

        auditService.record(
                organizationId,
                currentUserId,
                AuditAction.EVIDENCE_DOCUMENT_ARCHIVED,
                AuditResourceType.EVIDENCE_DOCUMENT,
                evidenceDocument.getId(),
                "Archived evidence document",
                """
                {"title":"%s"}
                """.formatted(evidenceDocument.getTitle())
        );
    }

    @Transactional
    public ComplianceItemEvidenceResponse linkEvidenceToComplianceItem(
            UUID organizationId,
            UUID complianceItemId,
            UUID currentUserId,
            LinkEvidenceRequest request
    ) {
        tenantAccessService.requireManagerRole(organizationId, currentUserId);

        CompanyComplianceItem complianceItem = complianceItemRepository
                .findByIdAndOrganization_Id(complianceItemId, organizationId)
                .orElseThrow(() -> new NotFoundException("Compliance item not found"));

        EvidenceDocument evidenceDocument = evidenceDocumentRepository
                .findByIdAndOrganization_Id(request.evidenceDocumentId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Evidence document not found"));

        if (evidenceDocument.getStatus() != EvidenceStatus.ACTIVE) {
            throw new ConflictException("Only active evidence can be linked");
        }

        if (evidenceLinkRepository.existsByComplianceItem_IdAndEvidenceDocument_Id(
                complianceItemId,
                request.evidenceDocumentId()
        )) {
            throw new ConflictException("Evidence is already linked to this compliance item");
        }

        User linkedByUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        ComplianceItemEvidenceLink link = evidenceLinkRepository.save(
                new ComplianceItemEvidenceLink(
                        complianceItem,
                        evidenceDocument,
                        linkedByUser
                )
        );

        auditService.record(
                organizationId,
                currentUserId,
                AuditAction.EVIDENCE_LINK_CREATED,
                AuditResourceType.EVIDENCE_LINK,
                link.getId(),
                "Linked evidence to compliance item",
                """
                {"complianceItemId":"%s","evidenceDocumentId":"%s"}
                """.formatted(
                        complianceItem.getId(),
                        evidenceDocument.getId()
                )
        );

        return toComplianceItemEvidenceResponse(link);
    }

    @Transactional(readOnly = true)
    public List<ComplianceItemEvidenceResponse> listEvidenceLinksForComplianceItem(
            UUID organizationId,
            UUID complianceItemId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        complianceItemRepository
                .findByIdAndOrganization_Id(complianceItemId, organizationId)
                .orElseThrow(() -> new NotFoundException("Compliance item not found"));

        return evidenceLinkRepository
                .findByComplianceItem_Id(complianceItemId)
                .stream()
                .map(this::toComplianceItemEvidenceResponse)
                .toList();
    }

    @Transactional
    public void unlinkEvidenceFromComplianceItem(
            UUID organizationId,
            UUID complianceItemId,
            UUID evidenceDocumentId,
            UUID currentUserId
    ) {
        tenantAccessService.requireManagerRole(organizationId, currentUserId);

        complianceItemRepository
                .findByIdAndOrganization_Id(complianceItemId, organizationId)
                .orElseThrow(() -> new NotFoundException("Compliance item not found"));

        evidenceDocumentRepository
                .findByIdAndOrganization_Id(evidenceDocumentId, organizationId)
                .orElseThrow(() -> new NotFoundException("Evidence document not found"));

        ComplianceItemEvidenceLink link = evidenceLinkRepository
                .findByComplianceItem_IdAndEvidenceDocument_Id(
                        complianceItemId,
                        evidenceDocumentId
                )
                .orElseThrow(() -> new NotFoundException("Evidence link not found"));

        auditService.record(
                organizationId,
                currentUserId,
                AuditAction.EVIDENCE_LINK_DELETED,
                AuditResourceType.EVIDENCE_LINK,
                link.getId(),
                "Unlinked evidence from compliance item",
                """
                {"complianceItemId":"%s","evidenceDocumentId":"%s"}
                """.formatted(
                        complianceItemId,
                        evidenceDocumentId
                )
        );

        evidenceLinkRepository.delete(link);
    }

    @Transactional(readOnly = true)
    public CreateEvidenceUploadUrlResponse createUploadUrl(
            UUID organizationId,
            UUID currentUserId,
            CreateEvidenceUploadUrlRequest request
    ) {
        tenantAccessService.requireManagerRole(organizationId, currentUserId);

        String objectKey = evidenceObjectKeyService.generateEvidenceObjectKey(
                organizationId,
                request.filename()
        );

        if (storageProperties.isSupabase()) {
            SupabaseSignedUploadResponse response =
                    supabaseStorageClient.createSignedUploadUrl(objectKey);

            return new CreateEvidenceUploadUrlResponse(
                    objectKey,
                    response.uploadUrl(),
                    "PUT",
                    secondsToMinutes(storageProperties.supabase().signedUrlExpirationSeconds())
            );
        }

        StorageService storageService = storageServiceProvider.getIfAvailable();

        if (storageService == null) {
            throw new ConflictException("MinIO storage service is not available");
        }

        String uploadUrl = storageService.createPresignedUploadUrl(
                objectKey,
                request.contentType()
        );

        return new CreateEvidenceUploadUrlResponse(
                objectKey,
                uploadUrl,
                "PUT",
                storageProperties.presignedUrlExpirationMinutes()
        );
    }

    @Transactional(readOnly = true)
    public EvidenceDownloadUrlResponse createDownloadUrl(
            UUID organizationId,
            UUID evidenceId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        EvidenceDocument evidenceDocument = evidenceDocumentRepository
                .findByIdAndOrganization_Id(evidenceId, organizationId)
                .orElseThrow(() -> new NotFoundException("Evidence document not found"));

        if (evidenceDocument.getSourceType() != EvidenceSourceType.FILE) {
            throw new ConflictException("Download URL is only available for file evidence");
        }

        if (evidenceDocument.getStatus() != EvidenceStatus.ACTIVE) {
            throw new ConflictException("Only active evidence can be downloaded");
        }

        if (storageProperties.isSupabase()) {
            SupabaseSignedDownloadResponse response =
                    supabaseStorageClient.createSignedDownloadUrl(
                            evidenceDocument.getFileObjectKey()
                    );

            return new EvidenceDownloadUrlResponse(
                    response.url(),
                    "GET",
                    secondsToMinutes(storageProperties.supabase().signedUrlExpirationSeconds())
            );
        }

        StorageService storageService = storageServiceProvider.getIfAvailable();

        if (storageService == null) {
            throw new ConflictException("MinIO storage service is not available");
        }

        String downloadUrl = storageService.createPresignedDownloadUrl(
                evidenceDocument.getFileObjectKey()
        );

        return new EvidenceDownloadUrlResponse(
                downloadUrl,
                "GET",
                storageProperties.presignedUrlExpirationMinutes()
        );
    }

    private int secondsToMinutes(int seconds) {
        return Math.max(1, (int) Math.ceil(seconds / 60.0));
    }

    private void validateEvidenceSource(
            EvidenceSourceType sourceType,
            String fileObjectKey,
            String externalUrl
    ) {
        if (sourceType == EvidenceSourceType.FILE && isBlank(fileObjectKey)) {
            throw new ConflictException("File evidence requires fileObjectKey");
        }

        if (sourceType == EvidenceSourceType.URL && isBlank(externalUrl)) {
            throw new ConflictException("URL evidence requires externalUrl");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private EvidenceDocumentResponse toEvidenceDocumentResponse(EvidenceDocument evidenceDocument) {
        return new EvidenceDocumentResponse(
                evidenceDocument.getId(),
                evidenceDocument.getOrganization().getId(),
                evidenceDocument.getTitle(),
                evidenceDocument.getDescription(),
                evidenceDocument.getEvidenceType(),
                evidenceDocument.getSourceType(),
                evidenceDocument.getFileObjectKey(),
                evidenceDocument.getExternalUrl(),
                evidenceDocument.getContentType(),
                evidenceDocument.getFileSizeBytes(),
                evidenceDocument.getUploadedByUser().getId(),
                evidenceDocument.getStatus(),
                evidenceDocument.getCreatedAt(),
                evidenceDocument.getUpdatedAt()
        );
    }

    private ComplianceItemEvidenceResponse toComplianceItemEvidenceResponse(
            ComplianceItemEvidenceLink link
    ) {
        return new ComplianceItemEvidenceResponse(
                link.getId(),
                link.getComplianceItem().getId(),
                toEvidenceDocumentResponse(link.getEvidenceDocument()),
                link.getLinkedByUser().getId(),
                link.getLinkedAt()
        );
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        return query.trim();
    }

    private static final Map<String, String> ALLOWED_EVIDENCE_SORT_FIELDS = Map.of(
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "title", "title",
            "evidenceType", "evidenceType",
            "sourceType", "sourceType"
    );
}
