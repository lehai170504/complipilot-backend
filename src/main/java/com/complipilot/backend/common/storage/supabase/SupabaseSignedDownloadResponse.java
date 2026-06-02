package com.complipilot.backend.common.storage.supabase;

import com.fasterxml.jackson.annotation.JsonAlias;

public record SupabaseSignedDownloadResponse(
        @JsonAlias({"signedUrl", "signedURL", "url"})
        String signedUrl
) {
    public String url() {
        if (signedUrl != null && !signedUrl.isBlank()) {
            return signedUrl;
        }

        throw new IllegalStateException("Supabase signed download URL response does not contain signedUrl/url");
    }
}