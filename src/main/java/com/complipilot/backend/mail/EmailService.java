package com.complipilot.backend.mail;

public interface EmailService {

    void sendInvitationEmail(
            String recipientEmail,
            String organizationName,
            String inviterEmail,
            String role,
            String acceptUrl
    );
}