package com.complipilot.backend.common.error;

import java.time.Instant;
import java.util.List;

import org.slf4j.MDC;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String requestId,
        List<FieldViolation> fieldViolations
) {

    public static ApiErrorResponse of(
            int status,
            String error,
            String message,
            String path
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                path,
                MDC.get("requestId"),
                List.of()
        );
    }

    public static ApiErrorResponse of(
            int status,
            String error,
            String message,
            String path,
            List<FieldViolation> fieldViolations
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                path,
                MDC.get("requestId"),
                fieldViolations
        );
    }

    public record FieldViolation(
            String field,
            String message
    ) {
    }
}