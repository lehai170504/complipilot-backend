package com.complipilot.backend.common.storage.supabase;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.complipilot.backend.common.storage.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class SupabaseStorageClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final StorageProperties storageProperties;

    public SupabaseStorageClient(
            ObjectMapper objectMapper,
            StorageProperties storageProperties
    ) {
        this.objectMapper = objectMapper;
        this.storageProperties = storageProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public SupabaseSignedUploadResponse createSignedUploadUrl(
            String objectKey
    ) {
        try {
            String url = normalizeUrl(storageProperties.supabase().url())
                    + "/storage/v1/object/upload/sign/"
                    + storageProperties.supabase().bucket()
                    + "/"
                    + objectKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + storageProperties.supabase().serviceRoleKey())
                    .header("apikey", storageProperties.supabase().serviceRoleKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Supabase signed upload URL failed: status="
                                + response.statusCode()
                                + ", body="
                                + response.body()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    SupabaseSignedUploadResponse.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create Supabase signed upload URL", exception);
        }
    }

    public SupabaseSignedDownloadResponse createSignedDownloadUrl(
            String objectKey
    ) {
        try {
            String url = normalizeUrl(storageProperties.supabase().url())
                    + "/storage/v1/object/sign/"
                    + storageProperties.supabase().bucket()
                    + "/"
                    + objectKey;

            String requestBody = objectMapper.writeValueAsString(
                    new SignedDownloadRequest(
                            storageProperties.supabase().signedUrlExpirationSeconds()
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + storageProperties.supabase().serviceRoleKey())
                    .header("apikey", storageProperties.supabase().serviceRoleKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Supabase signed download URL failed: status="
                                + response.statusCode()
                                + ", body="
                                + response.body()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    SupabaseSignedDownloadResponse.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create Supabase signed download URL", exception);
        }
    }

    private String normalizeUrl(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }

    private record SignedDownloadRequest(
            int expiresIn
    ) {
    }
}
