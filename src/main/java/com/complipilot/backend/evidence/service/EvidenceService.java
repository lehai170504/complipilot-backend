package com.complipilot.backend.evidence.service;

import java.util.List;
import java.util.UUID;

import com.complipilot.backend.common.error.ConflictException;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.compliance.entity.CompanyComplianceItem;
import com.complipilot.backend.compliance.repository.CompanyComplianceItemRepository;
import com.complipilot.backend.evidence.dto.ComplianceItemEvidenceResponse;
import com.complipilot.backend.evidence.dto.CreateEvidenceDocumentRequest;
import com.complipilot.backend.evidence.dto.EvidenceDocumentResponse;
import com.complipilot.backend.evidence.dto.LinkEvidenceRequest;
import com.complipilot.backend.evidence.dto.UpdateEvidenceDocumentRequest;
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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceService {

    private final EvidenceDocumentRepository evidenceDocumentRepository;
    private final ComplianceItemEvidenceLinkRepository evidenceLinkRepository;
    private final CompanyComplianceItemRepository complianceItemRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TenantAccessService tenantAccessService;

    public EvidenceService(
            EvidenceDocumentRepository evidenceDocumentRepository,
            ComplianceItemEvidenceLinkRepository evidenceLinkRepository,
            CompanyComplianceItemRepository complianceItemRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            TenantAccessService tenantAccessService
    ) {
        this.evidenceDocumentRepository = evidenceDocumentRepository;
        this.evidenceLinkRepository = evidenceLinkRepository;
        this.complianceItemRepository = complianceItemRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.tenantAccessService = tenantAccessService;
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

        return toEvidenceDocumentResponse(evidenceDocument);
    }

    @Transactional(readOnly = true)
    public List<EvidenceDocumentResponse> listEvidenceDocuments(
            UUID organizationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        return evidenceDocumentRepository
                .findByOrganization_IdAndStatusOrderByCreatedAtDesc(
                        organizationId,
                        EvidenceStatus.ACTIVE
                )
                .stream()
                .map(this::toEvidenceDocumentResponse)
                .toList();
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

        evidenceLinkRepository.delete(link);
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
}