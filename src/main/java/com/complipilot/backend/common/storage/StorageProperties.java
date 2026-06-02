package com.complipilot.backend.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String provider,
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String publicEndpoint,
        int presignedUrlExpirationMinutes,
        Supabase supabase
) {
    public record Supabase(
            String url,
            String serviceRoleKey,
            String bucket,
            int signedUrlExpirationSeconds
    ) {
    }

    public boolean isSupabase() {
        return "supabase".equalsIgnoreCase(provider);
    }

    public boolean isMinio() {
        return "minio".equalsIgnoreCase(provider);
    }
}