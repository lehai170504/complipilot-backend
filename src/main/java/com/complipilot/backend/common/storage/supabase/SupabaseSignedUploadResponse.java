package com.complipilot.backend.common.storage.supabase;

import com.fasterxml.jackson.annotation.JsonAlias;

public record SupabaseSignedUploadResponse(
        @JsonAlias({"signedUrl", "signedURL", "url"})
        String signedUrl,

        String path,

        String token
) {
    public String uploadUrl() {
        if (signedUrl != null && !signedUrl.isBlank()) {
            return signedUrl;
        }

        throw new IllegalStateException("Supabase signed upload URL response does not contain signedUrl/url");
    }
}