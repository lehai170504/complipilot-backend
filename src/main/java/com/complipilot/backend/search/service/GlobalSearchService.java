package com.complipilot.backend.search.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.complipilot.backend.audit.repository.AuditEventRepository;
import com.complipilot.backend.compliance.repository.CompanyComplianceItemRepository;
import com.complipilot.backend.evidence.repository.EvidenceDocumentRepository;
import com.complipilot.backend.search.dto.GlobalSearchResultDto;
import com.complipilot.backend.search.dto.SearchResultItemDto;
import com.complipilot.backend.task.repository.ComplianceTaskRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GlobalSearchService {

        private final CompanyComplianceItemRepository complianceItemRepository;
        private final EvidenceDocumentRepository evidenceDocumentRepository;
        private final ComplianceTaskRepository taskRepository;
        private final AuditEventRepository auditEventRepository;

        public GlobalSearchService(
                        CompanyComplianceItemRepository complianceItemRepository,
                        EvidenceDocumentRepository evidenceDocumentRepository,
                        ComplianceTaskRepository taskRepository,
                        AuditEventRepository auditEventRepository) {
                this.complianceItemRepository = complianceItemRepository;
                this.evidenceDocumentRepository = evidenceDocumentRepository;
                this.taskRepository = taskRepository;
                this.auditEventRepository = auditEventRepository;
        }

        public GlobalSearchResultDto search(UUID organizationId, String query) {
                if (query == null || query.trim().length() < 2) {
                        return new GlobalSearchResultDto(
                                        List.of(),
                                        List.of(),
                                        List.of(),
                                        List.of());
                }

                PageRequest pageRequest = PageRequest.of(0, 5);

                List<SearchResultItemDto> complianceItems = complianceItemRepository
                                .searchByOrganizationIdAndQuery(organizationId, query, pageRequest)
                                .stream()
                                .map(item -> new SearchResultItemDto(
                                                item.getId(),
                                                item.getRequirement().getTitle(),
                                                item.getNotes(),
                                                "/compliance/" + item.getId()))
                                .collect(Collectors.toList());

                List<SearchResultItemDto> evidence = evidenceDocumentRepository
                                .findByOrganizationIdWithFilters(organizationId, null, null, null, query, pageRequest)
                                .stream()
                                .map(doc -> new SearchResultItemDto(
                                                doc.getId(),
                                                doc.getTitle(),
                                                doc.getDescription(),
                                                "/evidence/" + doc.getId()))
                                .collect(Collectors.toList());

                List<SearchResultItemDto> tasks = taskRepository
                                .findByOrganizationIdWithFilters(organizationId, null, null, null, null, query,
                                                pageRequest)
                                .stream()
                                .map(task -> new SearchResultItemDto(
                                                task.getId(),
                                                task.getTitle(),
                                                task.getDescription(),
                                                "/tasks/" + task.getId()))
                                .collect(Collectors.toList());

                List<SearchResultItemDto> auditEvents = auditEventRepository
                                .findByOrganizationIdWithFilters(organizationId, null, null, query, pageRequest)
                                .stream()
                                .map(event -> new SearchResultItemDto(
                                                event.getId(),
                                                event.getAction().name(),
                                                event.getSummary(),
                                                "/audit"))
                                .collect(Collectors.toList());

                return new GlobalSearchResultDto(
                                complianceItems,
                                evidence,
                                tasks,
                                auditEvents);
        }
}
