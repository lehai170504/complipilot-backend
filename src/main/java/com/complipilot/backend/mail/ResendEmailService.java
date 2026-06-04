package com.complipilot.backend.mail;

import java.util.Map;

import com.complipilot.backend.common.error.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@ConditionalOnProperty(
        prefix = "app.mail",
        name = "enabled",
        havingValue = "true"
)
public class ResendEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private final MailProperties mailProperties;
    private final RestClient restClient;

    public ResendEmailService(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void sendInvitationEmail(
            String recipientEmail,
            String organizationName,
            String inviterEmail,
            String role,
            String acceptUrl
    ) {
        if (!mailProperties.isResend()) {
            throw new ConflictException("Unsupported mail provider: " + mailProperties.provider());
        }

        String apiKey = mailProperties.resend() == null
                ? null
                : mailProperties.resend().apiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new ConflictException("RESEND_API_KEY is required when mail is enabled");
        }

        try {
            restClient.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(Map.of(
                            "from", mailProperties.from(),
                            "to", recipientEmail,
                            "subject", "You have been invited to " + organizationName,
                            "html", buildInvitationHtml(
                                    organizationName,
                                    inviterEmail,
                                    role,
                                    acceptUrl
                            ),
                            "text", buildInvitationText(
                                    organizationName,
                                    inviterEmail,
                                    role,
                                    acceptUrl
                            )
                    ))
                    .retrieve()
                    .toBodilessEntity();

            log.info(
                    "Invitation email sent. recipientEmail={}, organizationName={}",
                    recipientEmail,
                    organizationName
            );
        } catch (RestClientResponseException exception) {
            log.warn(
                    "Failed to send invitation email. status={}, body={}",
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString()
            );

            throw new ConflictException("Failed to send invitation email");
        }
    }

    private String buildInvitationText(
            String organizationName,
            String inviterEmail,
            String role,
            String acceptUrl
    ) {
        return """
                You have been invited to join %s on CompliPilot.

                Invited by: %s
                Role: %s

                Accept invitation:
                %s

                This invitation link will expire soon.
                """.formatted(
                organizationName,
                inviterEmail,
                role,
                acceptUrl
        );
    }

    private String buildInvitationHtml(
            String organizationName,
            String inviterEmail,
            String role,
            String acceptUrl
    ) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:640px;margin:0 auto;padding:24px;color:#0f172a">
                  <div style="border:1px solid #e2e8f0;border-radius:18px;padding:24px;background:#ffffff">
                    <p style="margin:0 0 12px;font-size:12px;font-weight:700;letter-spacing:0.18em;text-transform:uppercase;color:#0891b2">
                      CompliPilot
                    </p>

                    <h1 style="margin:0;font-size:24px;line-height:1.3;color:#0f172a">
                      You have been invited to join %s
                    </h1>

                    <p style="margin:16px 0 0;font-size:14px;line-height:1.7;color:#475569">
                      %s invited you to join this workspace on CompliPilot.
                    </p>

                    <div style="margin:20px 0;padding:14px 16px;border-radius:14px;background:#f8fafc;border:1px solid #e2e8f0">
                      <p style="margin:0;font-size:14px;color:#475569">
                        Role: <strong style="color:#0f172a">%s</strong>
                      </p>
                    </div>

                    <a href="%s"
                       style="display:inline-block;margin-top:8px;padding:12px 18px;border-radius:12px;background:#67e8f9;color:#0f172a;text-decoration:none;font-weight:700">
                      Accept invitation
                    </a>

                    <p style="margin:20px 0 0;font-size:12px;line-height:1.6;color:#64748b">
                      If the button does not work, copy and paste this link into your browser:
                      <br />
                      <span style="word-break:break-all">%s</span>
                    </p>
                  </div>
                </div>
                """.formatted(
                escapeHtml(organizationName),
                escapeHtml(inviterEmail),
                escapeHtml(role),
                acceptUrl,
                acceptUrl
        );
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}