package com.complipilot.backend.export.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.complipilot.backend.audit.entity.AuditEvent;
import com.complipilot.backend.audit.repository.AuditEventRepository;
import com.complipilot.backend.compliance.entity.CompanyComplianceItem;
import com.complipilot.backend.compliance.repository.CompanyComplianceItemRepository;
import com.complipilot.backend.evidence.entity.EvidenceDocument;
import com.complipilot.backend.evidence.enums.EvidenceStatus;
import com.complipilot.backend.evidence.repository.EvidenceDocumentRepository;
import com.complipilot.backend.organization.service.TenantAccessService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CsvExportService {

    private static final int MAX_EXPORT_ROWS = 10_000;

    private final CompanyComplianceItemRepository complianceItemRepository;
    private final EvidenceDocumentRepository evidenceDocumentRepository;
    private final AuditEventRepository auditEventRepository;
    private final TenantAccessService tenantAccessService;

    public CsvExportService(
            CompanyComplianceItemRepository complianceItemRepository,
            EvidenceDocumentRepository evidenceDocumentRepository,
            AuditEventRepository auditEventRepository,
            TenantAccessService tenantAccessService
    ) {
        this.complianceItemRepository = complianceItemRepository;
        this.evidenceDocumentRepository = evidenceDocumentRepository;
        this.auditEventRepository = auditEventRepository;
        this.tenantAccessService = tenantAccessService;
    }

    @Transactional(readOnly = true)
    public byte[] exportComplianceItemsCsv(
            UUID organizationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        List<CompanyComplianceItem> items =
                complianceItemRepository.findByOrganization_Id(organizationId);

        StringBuilder csv = new StringBuilder();

        appendRow(
                csv,
                "id",
                "requirement_code",
                "requirement_title",
                "requirement_category",
                "status",
                "owner_email",
                "due_date",
                "notes"
        );

        for (CompanyComplianceItem item : items) {
            appendRow(
                    csv,
                    stringValue(item.getId()),
                    item.getRequirement() == null ? "" : item.getRequirement().getCode(),
                    item.getRequirement() == null ? "" : item.getRequirement().getTitle(),
                    item.getRequirement() == null ? "" : item.getRequirement().getCategory(),
                    enumValue(item.getStatus()),
                    item.getOwnerUser() == null ? "" : item.getOwnerUser().getEmail(),
                    localDateValue(item.getDueDate()),
                    item.getNotes()
            );
        }

        return toUtf8(csv);
    }

    @Transactional(readOnly = true)
    public byte[] exportEvidenceDocumentsCsv(
            UUID organizationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        List<EvidenceDocument> evidenceDocuments = evidenceDocumentRepository
                .findByOrganization_IdAndStatusNot(
                        organizationId,
                        EvidenceStatus.ARCHIVED,
                        PageRequest.of(
                                0,
                                MAX_EXPORT_ROWS,
                                Sort.by(Sort.Direction.DESC, "createdAt")
                        )
                )
                .getContent();

        StringBuilder csv = new StringBuilder();

        appendRow(
                csv,
                "id",
                "title",
                "description",
                "evidence_type",
                "source_type",
                "file_object_key",
                "external_url",
                "content_type",
                "file_size_bytes",
                "uploaded_by_email",
                "status",
                "created_at",
                "updated_at"
        );

        for (EvidenceDocument evidence : evidenceDocuments) {
            appendRow(
                    csv,
                    stringValue(evidence.getId()),
                    evidence.getTitle(),
                    evidence.getDescription(),
                    enumValue(evidence.getEvidenceType()),
                    enumValue(evidence.getSourceType()),
                    evidence.getFileObjectKey(),
                    evidence.getExternalUrl(),
                    evidence.getContentType(),
                    evidence.getFileSizeBytes() == null
                            ? ""
                            : String.valueOf(evidence.getFileSizeBytes()),
                    evidence.getUploadedByUser() == null
                            ? ""
                            : evidence.getUploadedByUser().getEmail(),
                    enumValue(evidence.getStatus()),
                    instantValue(evidence.getCreatedAt()),
                    instantValue(evidence.getUpdatedAt())
            );
        }

        return toUtf8(csv);
    }

    @Transactional(readOnly = true)
    public byte[] exportAuditEventsCsv(
            UUID organizationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        List<AuditEvent> auditEvents = auditEventRepository
                .findByOrganization_Id(
                        organizationId,
                        PageRequest.of(
                                0,
                                MAX_EXPORT_ROWS,
                                Sort.by(Sort.Direction.DESC, "createdAt")
                        )
                )
                .getContent();

        StringBuilder csv = new StringBuilder();

        appendRow(
                csv,
                "id",
                "actor_email",
                "action",
                "resource_type",
                "resource_id",
                "summary",
                "metadata_json",
                "created_at"
        );

        for (AuditEvent event : auditEvents) {
            appendRow(
                    csv,
                    stringValue(event.getId()),
                    event.getActorUser() == null ? "" : event.getActorUser().getEmail(),
                    enumValue(event.getAction()),
                    enumValue(event.getResourceType()),
                    stringValue(event.getResourceId()),
                    event.getSummary(),
                    event.getMetadataJson(),
                    instantValue(event.getCreatedAt())
            );
        }

        return toUtf8(csv);
    }

    private void appendRow(StringBuilder csv, String... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(",");
            }

            csv.append(escapeCsv(values[index]));
        }

        csv.append("\n");
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value
                .replace("\r\n", "\n")
                .replace("\r", "\n");

        boolean shouldQuote = normalized.contains(",")
                || normalized.contains("\"")
                || normalized.contains("\n");

        if (!shouldQuote) {
            return normalized;
        }

        return "\"" + normalized.replace("\"", "\"\"") + "\"";
    }

    private byte[] toUtf8(StringBuilder csv) {
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private String enumValue(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private String instantValue(Instant value) {
        return value == null ? "" : value.toString();
    }

    private String localDateValue(LocalDate value) {
        return value == null ? "" : value.toString();
    }
}