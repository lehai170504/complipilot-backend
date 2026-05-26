package com.complipilot.backend.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.complipilot.backend.common.error.UnauthorizedException;
import com.complipilot.backend.identity.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final String secret;
    private final String issuer;
    private final long accessTokenExpirationSeconds;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.access-token-expiration-seconds}") long accessTokenExpirationSeconds
    ) {
        this.secret = secret;
        this.issuer = issuer;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenExpirationSeconds);

        String headerJson = """
                {"alg":"HS256","typ":"JWT"}
                """;

        String payloadJson = """
                {"iss":"%s","sub":"%s","email":"%s","fullName":"%s","iat":%d,"exp":%d}
                """.formatted(
                escapeJson(issuer),
                user.getId(),
                escapeJson(user.getEmail()),
                escapeJson(user.getFullName()),
                now.getEpochSecond(),
                expiresAt.getEpochSecond()
        );

        String header = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String unsignedToken = header + "." + payload;
        String signature = sign(unsignedToken);

        return unsignedToken + "." + signature;
    }

    public AuthenticatedUser parseAndValidate(String token) {
        try {
            String[] parts = token.split("\\.");

            if (parts.length != 3) {
                throw new UnauthorizedException("Invalid access token");
            }

            String unsignedToken = parts[0] + "." + parts[1];
            String expectedSignature = sign(unsignedToken);

            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8)
            )) {
                throw new UnauthorizedException("Invalid access token");
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(payloadBytes);

            String tokenIssuer = payload.path("iss").asText();
            if (!issuer.equals(tokenIssuer)) {
                throw new UnauthorizedException("Invalid access token issuer");
            }

            long expiresAt = payload.path("exp").asLong();
            if (Instant.now().getEpochSecond() >= expiresAt) {
                throw new UnauthorizedException("Access token expired");
            }

            UUID userId = UUID.fromString(payload.path("sub").asText());
            String email = payload.path("email").asText();
            String fullName = payload.path("fullName").asText();

            return new AuthenticatedUser(userId, email, fullName);
        } catch (UnauthorizedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UnauthorizedException("Invalid access token");
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] signature = mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign JWT token", exception);
        }
    }

    private static String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}