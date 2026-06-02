package com.complipilot.backend.common.storage;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class EvidenceObjectKeyService {

    public String generateEvidenceObjectKey(UUID organizationId, String filename) {
        String safeFilename = sanitizeFilename(filename);

        return "organizations/%s/evidence/%s-%s".formatted(
                organizationId,
                UUID.randomUUID(),
                safeFilename
        );
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "evidence-file";
        }

        return filename
                .trim()
                .replace("\\", "-")
                .replace("/", "-")
                .replaceAll("[\\r\\n\\t]", "-");
    }
}