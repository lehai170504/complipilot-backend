package com.complipilot.backend.common.storage.supabase;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.complipilot.backend.common.storage.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Component
public class SupabaseStorageClient {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageClient.class);

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

    public SupabaseSignedUploadResponse createSignedUploadUrl(String objectKey) {
        try {
            String url = normalizeUrl(storageProperties.supabase().url())
                    + "/storage/v1/object/upload/sign/"
                    + encodePathSegment(storageProperties.supabase().bucket())
                    + "/"
                    + encodeObjectKey(objectKey);

            log.info(
                    "Creating Supabase signed upload URL. bucket={}, objectKey={}, url={}",
                    storageProperties.supabase().bucket(),
                    objectKey,
                    maskSignedStorageUrl(url)
            );

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

            log.info(
                    "Supabase signed upload response. status={}, body={}",
                    response.statusCode(),
                    response.body()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Supabase signed upload URL failed: status="
                                + response.statusCode()
                                + ", body="
                                + response.body()
                );
            }

            SupabaseSignedUploadResponse signedUploadResponse = objectMapper.readValue(
                    response.body(),
                    SupabaseSignedUploadResponse.class
            );

            String signedUrl = normalizeSignedStorageUrl(
                    signedUploadResponse.uploadUrl()
            );

            log.info(
                    "Supabase signed upload URL created. objectKey={}, responsePath={}, hasSignedUrl={}, hasToken={}",
                    objectKey,
                    signedUploadResponse.path(),
                    signedUrl != null && !signedUrl.isBlank(),
                    signedUploadResponse.token() != null && !signedUploadResponse.token().isBlank()
            );

            return new SupabaseSignedUploadResponse(
                    signedUrl,
                    signedUploadResponse.path(),
                    signedUploadResponse.token()
            );
        } catch (Exception exception) {
            log.error(
                    "Failed to create Supabase signed upload URL. bucket={}, objectKey={}",
                    safeBucket(),
                    objectKey,
                    exception
            );
            throw new IllegalStateException("Failed to create Supabase signed upload URL", exception);
        }
    }

    public SupabaseSignedDownloadResponse createSignedDownloadUrl(String objectKey) {
        try {
            String url = normalizeUrl(storageProperties.supabase().url())
                    + "/storage/v1/object/sign/"
                    + encodePathSegment(storageProperties.supabase().bucket())
                    + "/"
                    + encodeObjectKey(objectKey);

            String requestBody = objectMapper.writeValueAsString(
                    Map.of(
                            "expiresIn",
                            storageProperties.supabase().signedUrlExpirationSeconds()
                    )
            );

            log.info(
                    "Creating Supabase signed download URL. bucket={}, objectKey={}, url={}, expiresInSeconds={}",
                    storageProperties.supabase().bucket(),
                    objectKey,
                    maskSignedStorageUrl(url),
                    storageProperties.supabase().signedUrlExpirationSeconds()
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

            log.info(
                    "Supabase signed download response. status={}, body={}",
                    response.statusCode(),
                    response.body()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Supabase signed download URL failed: status="
                                + response.statusCode()
                                + ", body="
                                + response.body()
                );
            }

            SupabaseSignedDownloadResponse signedDownloadResponse = objectMapper.readValue(
                    response.body(),
                    SupabaseSignedDownloadResponse.class
            );

            String signedUrl = normalizeSignedStorageUrl(
                    signedDownloadResponse.url()
            );

            log.info(
                    "Supabase signed download URL created. objectKey={}, hasUrl={}",
                    objectKey,
                    signedUrl != null && !signedUrl.isBlank()
            );

            return new SupabaseSignedDownloadResponse(signedUrl);
        } catch (Exception exception) {
            log.error(
                    "Failed to create Supabase signed download URL. bucket={}, objectKey={}",
                    safeBucket(),
                    objectKey,
                    exception
            );
            throw new IllegalStateException("Failed to create Supabase signed download URL", exception);
        }
    }

    private String normalizeUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("SUPABASE_URL must not be blank");
        }

        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }

    private String encodeObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalStateException("Supabase objectKey must not be blank");
        }

        return Arrays.stream(objectKey.split("/"))
                .map(this::encodePathSegment)
                .collect(Collectors.joining("/"));
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private String safeBucket() {
        try {
            return storageProperties.supabase().bucket();
        } catch (Exception exception) {
            return "<unknown>";
        }
    }

    private String maskSignedStorageUrl(String url) {
        int queryIndex = url.indexOf('?');

        if (queryIndex < 0) {
            return url;
        }

        return url.substring(0, queryIndex) + "?<masked>";
    }

    private String normalizeSignedStorageUrl(String signedUrl) {
        String baseUrl = normalizeUrl(storageProperties.supabase().url());

        if (signedUrl == null || signedUrl.isBlank()) {
            throw new IllegalStateException("Supabase signed URL must not be blank");
        }

        if (signedUrl.startsWith("http://") || signedUrl.startsWith("https://")) {
            return signedUrl;
        }

        if (signedUrl.startsWith("/storage/v1/")) {
            return baseUrl + signedUrl;
        }

        if (signedUrl.startsWith("/object/")) {
            return baseUrl + "/storage/v1" + signedUrl;
        }

        if (signedUrl.startsWith("object/")) {
            return baseUrl + "/storage/v1/" + signedUrl;
        }

        return baseUrl + "/storage/v1/" + signedUrl;
    }
}