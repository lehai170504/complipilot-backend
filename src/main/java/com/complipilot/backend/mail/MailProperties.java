package com.complipilot.backend.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        boolean enabled,
        String provider,
        String from,
        Resend resend
) {
    public record Resend(
            String apiKey
    ) {
    }

    public boolean isResend() {
        return "resend".equalsIgnoreCase(provider);
    }
}