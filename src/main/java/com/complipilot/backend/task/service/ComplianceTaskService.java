package com.complipilot.backend.task.service;

import com.complipilot.backend.audit.enums.AuditAction;
import com.complipilot.backend.audit.enums.AuditResourceType;
import com.complipilot.backend.audit.service.AuditService;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.compliance.entity.CompanyComplianceItem;
import com.complipilot.backend.compliance.repository.CompanyComplianceItemRepository;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;
import com.complipilot.backend.organization.entity.Organization;
import com.complipilot.backend.organization.repository.OrganizationRepository;
import com.complipilot.backend.organization.service.TenantAccessService;
import com.complipilot.backend.task.dto.ComplianceTaskResponse;
import com.complipilot.backend.task.dto.ComplianceTaskSummaryResponse;
import com.complipilot.backend.task.dto.CreateComplianceTaskRequest;
import com.complipilot.backend.task.dto.UpdateComplianceTaskRequest;
import com.complipilot.backend.task.entity.ComplianceTask;
import com.complipilot.backend.task.enums.ComplianceTaskStatus;
import com.complipilot.backend.task.repository.ComplianceTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ComplianceTaskService {

    private static final Set<ComplianceTaskStatus> DONE_STATUSES = EnumSet.of(
            ComplianceTaskStatus.DONE,
            ComplianceTaskStatus.CANCELLED
    );

    private final ComplianceTaskRepository complianceTaskRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final CompanyComplianceItemRepository complianceItemRepository;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;

    public ComplianceTaskService(
            ComplianceTaskRepository complianceTaskRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            CompanyComplianceItemRepository complianceItemRepository,
            TenantAccessService tenantAccessService,
            AuditService auditService
    ) {
        this.complianceTaskRepository = complianceTaskRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.complianceItemRepository = complianceItemRepository;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
    }

    @Transactional
    public ComplianceTaskResponse createTask(
            UUID organizationId,
            UUID currentUserId,
            CreateComplianceTaskRequest request
    ) {
        tenantAccessService.requireManagerRole(organizationId, currentUserId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        User createdByUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        CompanyComplianceItem complianceItem = null;
        if (request.complianceItemId() != null) {
            complianceItem = complianceItemRepository
                    .findByIdAndOrganization_Id(request.complianceItemId(), organizationId)
                    .orElseThrow(() -> new NotFoundException("Compliance item not found"));
        }

        User assigneeUser = null;
        if (request.assigneeUserId() != null) {
            tenantAccessService.requireActiveMember(organizationId, request.assigneeUserId());

            assigneeUser = userRepository.findById(request.assigneeUserId())
                    .orElseThrow(() -> new NotFoundException("Assignee user not found"));
        }

        ComplianceTask task = complianceTaskRepository.save(
                new ComplianceTask(
                        organization,
                        complianceItem,
                        request.title(),
                        request.description(),
                        assigneeUser,
                        createdByUser,
                        request.priority(),
                        request.dueDate()
                )
        );

        auditService.record(
                organizationId,
                currentUserId,
                AuditAction.COMPLIANCE_TASK_CREATED,
                AuditResourceType.COMPLIANCE_TASK,
                task.getId(),
                "Created compliance task",
                """
                {"title":"%s","status":"%s","priority":"%s"}
                """.formatted(task.getTitle(), task.getStatus(), task.getPriority())
        );

        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<ComplianceTaskResponse> listTasks(
            UUID organizationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        return complianceTaskRepository
                .findByOrganization_IdOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComplianceTaskSummaryResponse getTaskSummary(
            UUID organizationId,
            UUID currentUserId
    ) {
        tenantAccessService.requireActiveMember(organizationId, currentUserId);

        LocalDate today = LocalDate.now();

        long overdue = complianceTaskRepository
                .findByOrganization_IdAndDueDateBeforeAndStatusNotInOrderByDueDateAsc(
                        organizationId,
                        today,
                        DONE_STATUSES
                )
                .size();

        return new ComplianceTaskSummaryResponse(
                organizationId,
                complianceTaskRepository.countByOrganization_Id(organizationId),
                complianceTaskRepository.countByOrganization_IdAndStatus(
                        organizationId,
                        ComplianceTaskStatus.OPEN
                ),
                complianceTaskRepository.countByOrganization_IdAndStatus(
                        organizationId,
                        ComplianceTaskStatus.IN_PROGRESS
                ),
                complianceTaskRepository.countByOrganization_IdAndStatus(
                        organizationId,
                        ComplianceTaskStatus.DONE
                ),
                complianceTaskRepository.countByOrganization_IdAndStatus(
                        organizationId,
                        ComplianceTaskStatus.CANCELLED
                ),
                overdue
        );
    }

    @Transactional
    public ComplianceTaskResponse updateTask(
            UUID organizationId,
            UUID taskId,
            UUID currentUserId,
            UpdateComplianceTaskRequest request
    ) {
        tenantAccessService.requireManagerRole(organizationId, currentUserId);

        ComplianceTask task = complianceTaskRepository
                .findByIdAndOrganization_Id(taskId, organizationId)
                .orElseThrow(() -> new NotFoundException("Compliance task not found"));

        ComplianceTaskStatus oldStatus = task.getStatus();

        User assigneeUser = null;
        if (request.assigneeUserId() != null) {
            tenantAccessService.requireActiveMember(organizationId, request.assigneeUserId());

            assigneeUser = userRepository.findById(request.assigneeUserId())
                    .orElseThrow(() -> new NotFoundException("Assignee user not found"));
        }

        task.updateDetails(
                request.title(),
                request.description(),
                assigneeUser,
                request.priority(),
                request.dueDate()
        );

        if (request.status() != null) {
            task.updateStatus(request.status());
        }

        auditService.record(
                organizationId,
                currentUserId,
                AuditAction.COMPLIANCE_TASK_UPDATED,
                AuditResourceType.COMPLIANCE_TASK,
                task.getId(),
                "Updated compliance task",
                """
                {"oldStatus":"%s","newStatus":"%s","title":"%s"}
                """.formatted(oldStatus, task.getStatus(), task.getTitle())
        );

        return toResponse(task);
    }

    @Transactional
    public void deleteTask(
            UUID organizationId,
            UUID taskId,
            UUID currentUserId
    ) {
        tenantAccessService.requireManagerRole(organizationId, currentUserId);

        ComplianceTask task = complianceTaskRepository
                .findByIdAndOrganization_Id(taskId, organizationId)
                .orElseThrow(() -> new NotFoundException("Compliance task not found"));

        auditService.record(
                organizationId,
                currentUserId,
                AuditAction.COMPLIANCE_TASK_DELETED,
                AuditResourceType.COMPLIANCE_TASK,
                task.getId(),
                "Deleted compliance task",
                """
                {"title":"%s","status":"%s"}
                """.formatted(task.getTitle(), task.getStatus())
        );

        complianceTaskRepository.delete(task);
    }

    private ComplianceTaskResponse toResponse(ComplianceTask task) {
        UUID complianceItemId = task.getComplianceItem() == null
                ? null
                : task.getComplianceItem().getId();

        UUID assigneeUserId = task.getAssigneeUser() == null
                ? null
                : task.getAssigneeUser().getId();

        String assigneeEmail = task.getAssigneeUser() == null
                ? null
                : task.getAssigneeUser().getEmail();

        return new ComplianceTaskResponse(
                task.getId(),
                task.getOrganization().getId(),
                complianceItemId,
                task.getTitle(),
                task.getDescription(),
                assigneeUserId,
                assigneeEmail,
                task.getCreatedByUser().getId(),
                task.getCreatedByUser().getEmail(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
