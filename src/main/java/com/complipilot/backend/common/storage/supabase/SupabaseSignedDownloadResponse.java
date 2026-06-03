package com.complipilot.backend.common.storage.supabase;

import com.fasterxml.jackson.annotation.JsonAlias;

public record SupabaseSignedDownloadResponse(
        @JsonAlias({"signedURL", "signedUrl", "url"})
        String signedUrl
) {
    public String url() {
        if (signedUrl == null || signedUrl.isBlank()) {
            throw new IllegalStateException(
                    "Supabase signed download URL response does not contain signedURL/signedUrl/url"
            );
        }

        return signedUrl;
    }
}