package com.complipilot.backend.common.storage;

import io.minio.MinioClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "app.storage",
        name = "provider",
        havingValue = "minio",
        matchIfMissing = true
)
public class MinioConfig {

    @Bean
    MinioClient minioClient(StorageProperties storageProperties) {
        return MinioClient.builder()
                .endpoint(storageProperties.endpoint())
                .credentials(
                        storageProperties.accessKey(),
                        storageProperties.secretKey()
                )
                .build();
    }
}