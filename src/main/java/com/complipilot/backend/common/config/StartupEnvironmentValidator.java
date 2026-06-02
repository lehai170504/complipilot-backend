package com.complipilot.backend.common.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupEnvironmentValidator implements ApplicationRunner {

    private static final String PROD_PROFILE = "prod";
    private static final String STORAGE_PROVIDER_SUPABASE = "supabase";
    private static final String STORAGE_PROVIDER_MINIO = "minio";

    private final Environment environment;

    public StartupEnvironmentValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfileActive()) {
            return;
        }

        require("DATABASE_URL");
        require("DATABASE_USERNAME");
        require("DATABASE_PASSWORD");

        require("APP_CORS_ALLOWED_ORIGINS");

        require("JWT_SECRET");
        require("JWT_ISSUER");

        require("AI_SERVICE_BASE_URL");

        validateStorageProvider();
        validateJwtSecret();
        validateCorsOrigins();
    }

    private boolean isProdProfileActive() {
        return Arrays.asList(environment.getActiveProfiles())
                .contains(PROD_PROFILE);
    }

    private void validateStorageProvider() {
        String storageProvider = environment.getProperty(
                "STORAGE_PROVIDER",
                environment.getProperty("app.storage.provider", STORAGE_PROVIDER_MINIO)
        );

        if (STORAGE_PROVIDER_SUPABASE.equalsIgnoreCase(storageProvider)) {
            require("SUPABASE_URL");
            require("SUPABASE_SERVICE_ROLE_KEY");
            require("SUPABASE_STORAGE_BUCKET");
            return;
        }

        if (STORAGE_PROVIDER_MINIO.equalsIgnoreCase(storageProvider)) {
            require("MINIO_ENDPOINT");
            require("MINIO_PUBLIC_ENDPOINT");
            require("MINIO_ACCESS_KEY");
            require("MINIO_SECRET_KEY");
            require("MINIO_BUCKET_EVIDENCE");
            return;
        }

        throw new StartupValidationException(
                "Unsupported STORAGE_PROVIDER in production: " + storageProvider
                        + ". Supported values: minio, supabase"
        );
    }

    private void require(String propertyName) {
        String value = environment.getProperty(propertyName);

        if (value == null || value.isBlank()) {
            throw new StartupValidationException(
                    "Missing required production environment variable: " + propertyName
            );
        }
    }

    private void validateJwtSecret() {
        String jwtSecret = environment.getProperty("JWT_SECRET");

        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new StartupValidationException(
                    "JWT_SECRET must be at least 32 characters long in production"
            );
        }

        List<String> unsafeSecrets = List.of(
                "local-dev-secret-key-change-this-in-production-please-123456",
                "local-prod-test-secret-key-change-this-please-1234567890",
                "change_this_to_a_very_long_random_secret_at_least_64_chars",
                "CHANGE_THIS_TO_A_LONG_RANDOM_SECRET_AT_LEAST_64_CHARS"
        );

        if (unsafeSecrets.contains(jwtSecret)) {
            throw new StartupValidationException(
                    "JWT_SECRET is using a known unsafe example value. Set a real production secret."
            );
        }
    }

    private void validateCorsOrigins() {
        String corsOrigins = environment.getProperty("APP_CORS_ALLOWED_ORIGINS");

        if (corsOrigins == null || corsOrigins.isBlank()) {
            throw new StartupValidationException(
                    "APP_CORS_ALLOWED_ORIGINS must not be blank in production"
            );
        }

        if (corsOrigins.contains("*")) {
            throw new StartupValidationException(
                    "APP_CORS_ALLOWED_ORIGINS must not contain '*' in production"
            );
        }

        if (corsOrigins.endsWith("/")) {
            throw new StartupValidationException(
                    "APP_CORS_ALLOWED_ORIGINS must not end with '/' in production"
            );
        }
    }
}