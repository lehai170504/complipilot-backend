package com.complipilot.backend.common.storage.supabase;

public record SupabaseSignedUploadResponse(
        String signedUrl,
        String path,
        String token
) {
}
