package com.complipilot.backend.common.storage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.http.Method;

import org.springframework.stereotype.Service;

@Service
public class StorageService {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    public StorageService(
            MinioClient minioClient,
            StorageProperties storageProperties
    ) {
        this.minioClient = minioClient;
        this.storageProperties = storageProperties;
    }

    public String generateEvidenceObjectKey(
            UUID organizationId,
            String originalFilename
    ) {
        String safeFilename = sanitizeFilename(originalFilename);
        return "organizations/%s/evidence/%s-%s".formatted(
                organizationId,
                UUID.randomUUID(),
                safeFilename
        );
    }

    public String createPresignedUploadUrl(
            String objectKey,
            String contentType
    ) {
        ensureBucketExists();

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(storageProperties.bucket())
                            .object(objectKey)
                            .expiry(
                                    storageProperties.presignedUrlExpirationMinutes(),
                                    TimeUnit.MINUTES
                            )
                            .extraHeaders(Map.of("Content-Type", contentType))
                            .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create presigned upload URL", exception);
        }
    }

    public String createPresignedDownloadUrl(String objectKey) {
        ensureBucketExists();

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(storageProperties.bucket())
                            .object(objectKey)
                            .expiry(
                                    storageProperties.presignedUrlExpirationMinutes(),
                                    TimeUnit.MINUTES
                            )
                            .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create presigned download URL", exception);
        }
    }

    public String bucket() {
        return storageProperties.bucket();
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(storageProperties.bucket())
                            .build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(storageProperties.bucket())
                                .build()
                );
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize evidence storage bucket", exception);
        }
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload.bin";
        }

        return originalFilename
                .trim()
                .replaceAll("[^a-zA-Z0-9._-]", "-")
                .replaceAll("-{2,}", "-");
    }
}