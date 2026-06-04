package com.complipilot.backend.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "app.mail",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class NoopEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(NoopEmailService.class);

    @Override
    public void sendInvitationEmail(
            String recipientEmail,
            String organizationName,
            String inviterEmail,
            String role,
            String acceptUrl
    ) {
        log.info(
                "Mail disabled. Invitation email skipped. recipientEmail={}, organizationName={}, acceptUrl={}",
                recipientEmail,
                organizationName,
                acceptUrl
        );
    }
}