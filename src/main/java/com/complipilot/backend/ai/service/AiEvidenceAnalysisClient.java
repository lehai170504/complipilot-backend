package com.complipilot.backend.ai.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.complipilot.backend.ai.config.AiProperties;
import com.complipilot.backend.ai.dto.EvidenceAiAnalysisRequest;
import com.complipilot.backend.ai.dto.EvidenceAiAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AiEvidenceAnalysisClient {

    private static final Logger log =
            LoggerFactory.getLogger(AiEvidenceAnalysisClient.class);

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

        log.info("AI service client configured. baseUrl={}", this.baseUrl);
    }

    public EvidenceAiAnalysisResponse analyzeEvidence(
            EvidenceAiAnalysisRequest request
    ) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            URI uri = URI.create(baseUrl + "/api/v1/ai/evidence/analyze");

            log.info("Calling AI service. uri={}, body={}", uri, requestJson);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, java.nio.charset.StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            log.info(
                    "AI service response. status={}, body={}",
                    response.statusCode(),
                    response.body()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "AI service returned error: status="
                                + response.statusCode()
                                + ", body="
                                + response.body()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    EvidenceAiAnalysisResponse.class
            );
        } catch (Exception exception) {
            log.error("Unexpected AI client failure", exception);
            throw new IllegalStateException("Unexpected AI client failure", exception);
        }
    }
}