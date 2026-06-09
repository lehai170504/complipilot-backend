package com.complipilot.backend.system.service;

import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.complipilot.backend.ai.config.AiProperties;
import com.complipilot.backend.common.storage.StorageProperties;
import com.complipilot.backend.mail.MailProperties;
import com.complipilot.backend.system.dto.SystemStatusComponentResponse;
import com.complipilot.backend.system.dto.SystemStatusResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SystemStatusService {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_DOWN = "DOWN";

    private final DataSource dataSource;
    private final StorageProperties storageProperties;
    private final AiProperties aiProperties;
    private final MailProperties mailProperties;
    private final String appVersion;

    public SystemStatusService(
            DataSource dataSource,
            StorageProperties storageProperties,
            AiProperties aiProperties,
            MailProperties mailProperties,
            @Value("${info.app.version:unknown}") String appVersion
    ) {
        this.dataSource = dataSource;
        this.storageProperties = storageProperties;
        this.aiProperties = aiProperties;
        this.mailProperties = mailProperties;
        this.appVersion = appVersion;
    }

    public SystemStatusResponse getStatus() {
        List<SystemStatusComponentResponse> components = List.of(
                backendComponent(),
                databaseComponent(),
                storageComponent(),
                aiComponent(),
                mailComponent()
        );

        String overallStatus = components.stream()
                .anyMatch(component -> STATUS_DOWN.equals(component.status()))
                ? STATUS_DOWN
                : components.stream().anyMatch(component -> STATUS_WARN.equals(component.status()))
                ? STATUS_WARN
                : STATUS_UP;

        return new SystemStatusResponse(
                overallStatus,
                "complipilot-backend",
                appVersion,
                Instant.now(),
                components
        );
    }

    private SystemStatusComponentResponse backendComponent() {
        return new SystemStatusComponentResponse(
                "backend",
                "Backend API",
                STATUS_UP,
                "Backend API is running.",
                Map.of(
                        "service", "complipilot-backend",
                        "version", appVersion
                )
        );
    }

    private SystemStatusComponentResponse databaseComponent() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);

            if (!valid) {
                return new SystemStatusComponentResponse(
                        "database",
                        "Database",
                        STATUS_DOWN,
                        "Database connection is not valid.",
                        Map.of()
                );
            }

            return new SystemStatusComponentResponse(
                    "database",
                    "Database",
                    STATUS_UP,
                    "Database connection is healthy.",
                    Map.of(
                            "databaseProductName",
                            connection.getMetaData().getDatabaseProductName(),
                            "databaseProductVersion",
                            connection.getMetaData().getDatabaseProductVersion()
                    )
            );
        } catch (Exception exception) {
            return new SystemStatusComponentResponse(
                    "database",
                    "Database",
                    STATUS_DOWN,
                    "Database health check failed.",
                    Map.of(
                            "error", exception.getClass().getSimpleName()
                    )
            );
        }
    }

    private SystemStatusComponentResponse storageComponent() {
        String provider = blankToUnknown(storageProperties.provider());

        if (storageProperties.isSupabase()) {
            boolean configured = hasText(storageProperties.supabase().url())
                    && hasText(storageProperties.supabase().serviceRoleKey())
                    && hasText(storageProperties.supabase().bucket());

            return new SystemStatusComponentResponse(
                    "storage",
                    "Evidence Storage",
                    configured ? STATUS_UP : STATUS_WARN,
                    configured
                            ? "Supabase storage is configured."
                            : "Supabase storage is selected but missing configuration.",
                    Map.of(
                            "provider", provider,
                            "bucket", storageProperties.supabase().bucket(),
                            "configured", configured
                    )
            );
        }

        if (storageProperties.isMinio()) {
            boolean configured = hasText(storageProperties.endpoint())
                    && hasText(storageProperties.bucket());

            return new SystemStatusComponentResponse(
                    "storage",
                    "Evidence Storage",
                    configured ? STATUS_UP : STATUS_WARN,
                    configured
                            ? "MinIO storage is configured."
                            : "MinIO storage is selected but missing configuration.",
                    Map.of(
                            "provider", provider,
                            "endpoint", blankToUnknown(storageProperties.endpoint()),
                            "bucket", blankToUnknown(storageProperties.bucket()),
                            "configured", configured
                    )
            );
        }

        return new SystemStatusComponentResponse(
                "storage",
                "Evidence Storage",
                STATUS_WARN,
                "Storage provider is unknown.",
                Map.of(
                        "provider", provider,
                        "configured", false
                )
        );
    }

    private SystemStatusComponentResponse aiComponent() {
        boolean configured = hasText(aiProperties.baseUrl());

        return new SystemStatusComponentResponse(
                "ai",
                "AI Service",
                configured ? STATUS_UP : STATUS_WARN,
                configured
                        ? "AI service base URL is configured."
                        : "AI service base URL is missing.",
                Map.of(
                        "baseUrl", blankToUnknown(aiProperties.baseUrl()),
                        "configured", configured
                )
        );
    }

    private SystemStatusComponentResponse mailComponent() {
        boolean enabled = mailProperties.enabled();
        boolean apiKeyConfigured = mailProperties.resend() != null
                && hasText(mailProperties.resend().apiKey());

        boolean configured = !enabled || (
                mailProperties.isResend()
                        && hasText(mailProperties.from())
                        && apiKeyConfigured
        );

        String status = configured ? STATUS_UP : STATUS_WARN;

        return new SystemStatusComponentResponse(
                "mail",
                "Mail Provider",
                status,
                enabled
                        ? configured
                        ? "Mail delivery is enabled and configured."
                        : "Mail delivery is enabled but missing configuration."
                        : "Mail delivery is disabled.",
                Map.of(
                        "enabled", enabled,
                        "provider", blankToUnknown(mailProperties.provider()),
                        "from", blankToUnknown(mailProperties.from()),
                        "apiKeyConfigured", apiKeyConfigured
                )
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToUnknown(String value) {
        return hasText(value) ? value : "unknown";
    }
}