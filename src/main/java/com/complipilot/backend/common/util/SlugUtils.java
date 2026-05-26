package com.complipilot.backend.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

public final class SlugUtils {

    private SlugUtils() {
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "org-" + shortRandomSuffix();
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");

        if (normalized.isBlank()) {
            return "org-" + shortRandomSuffix();
        }

        return normalized;
    }

    public static String withRandomSuffix(String baseSlug) {
        return baseSlug + "-" + shortRandomSuffix();
    }

    private static String shortRandomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}