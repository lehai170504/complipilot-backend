package com.complipilot.backend.ai.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.complipilot.backend.ai.config.AiProperties;
import com.complipilot.backend.ai.dto.ComplianceEvidenceSuggestionRequest;
import com.complipilot.backend.ai.dto.ComplianceEvidenceSuggestionResponse;
import com.complipilot.backend.ai.dto.EvidenceAiAnalysisRequest;
import com.complipilot.backend.ai.dto.EvidenceAiAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class AiEvidenceAnalysisClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public AiEvidenceAnalysisClient(
            AiProperties aiProperties,
            ObjectMapper objectMapper
    ) {
        this.baseUrl = aiProperties.baseUrl();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public EvidenceAiAnalysisResponse analyzeEvidence(
            EvidenceAiAnalysisRequest request
    ) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            URI uri = URI.create(baseUrl + "/api/v1/ai/evidence/analyze");

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            requestJson,
                            StandardCharsets.UTF_8
                    ))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "AI service returned error: status=" + response.statusCode()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    EvidenceAiAnalysisResponse.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException("AI evidence analysis failed", exception);
        }
    }

    public ComplianceEvidenceSuggestionResponse suggestComplianceEvidence(
            ComplianceEvidenceSuggestionRequest request
    ) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            URI uri = URI.create(baseUrl + "/api/v1/ai/compliance/suggest-evidence");

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            requestJson,
                            StandardCharsets.UTF_8
                    ))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "AI service returned error: status=" + response.statusCode()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    ComplianceEvidenceSuggestionResponse.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException("AI compliance evidence suggestion failed", exception);
        }
    }
}