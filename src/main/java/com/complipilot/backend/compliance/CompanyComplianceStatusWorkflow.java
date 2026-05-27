package com.complipilot.backend.compliance;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.complipilot.backend.common.error.BadRequestException;
import com.complipilot.backend.compliance.enums.CompanyComplianceStatus;

public final class CompanyComplianceStatusWorkflow {

    private static final Map<CompanyComplianceStatus, Set<CompanyComplianceStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(CompanyComplianceStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(
                CompanyComplianceStatus.OPEN,
                EnumSet.of(
                        CompanyComplianceStatus.IN_PROGRESS,
                        CompanyComplianceStatus.WAIVED
                )
        );

        ALLOWED_TRANSITIONS.put(
                CompanyComplianceStatus.IN_PROGRESS,
                EnumSet.of(
                        CompanyComplianceStatus.READY_FOR_REVIEW,
                        CompanyComplianceStatus.WAIVED
                )
        );

        ALLOWED_TRANSITIONS.put(
                CompanyComplianceStatus.READY_FOR_REVIEW,
                EnumSet.of(
                        CompanyComplianceStatus.IN_PROGRESS,
                        CompanyComplianceStatus.COMPLIANT,
                        CompanyComplianceStatus.NON_COMPLIANT,
                        CompanyComplianceStatus.WAIVED
                )
        );

        ALLOWED_TRANSITIONS.put(
                CompanyComplianceStatus.NON_COMPLIANT,
                EnumSet.of(
                        CompanyComplianceStatus.IN_PROGRESS,
                        CompanyComplianceStatus.WAIVED
                )
        );

        ALLOWED_TRANSITIONS.put(
                CompanyComplianceStatus.COMPLIANT,
                EnumSet.of(
                        CompanyComplianceStatus.IN_PROGRESS
                )
        );

        ALLOWED_TRANSITIONS.put(
                CompanyComplianceStatus.WAIVED,
                EnumSet.of(
                        CompanyComplianceStatus.OPEN
                )
        );
    }

    private CompanyComplianceStatusWorkflow() {
    }

    public static void validateTransition(
            CompanyComplianceStatus currentStatus,
            CompanyComplianceStatus nextStatus
    ) {
        if (nextStatus == null) {
            return;
        }

        if (currentStatus == nextStatus) {
            return;
        }

        Set<CompanyComplianceStatus> allowedNextStatuses =
                ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!allowedNextStatuses.contains(nextStatus)) {
            throw new BadRequestException(
                    "Invalid compliance status transition from %s to %s"
                            .formatted(currentStatus, nextStatus)
            );
        }
    }
}