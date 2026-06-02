package com.complipilot.backend.common.storage.supabase;

public record SupabaseSignedDownloadResponse(
        String signedURL,
        String signedUrl
) {
    public String url() {
        if (signedUrl != null && !signedUrl.isBlank()) {
            return signedUrl;
        }

        return signedURL;
    }
}