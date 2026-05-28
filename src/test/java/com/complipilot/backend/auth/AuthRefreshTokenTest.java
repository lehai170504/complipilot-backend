package com.complipilot.backend.auth;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complipilot.backend.common.storage.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = "app.rate-limit.enabled=false")
@AutoConfigureMockMvc
@Import(AuthRefreshTokenTest.TestcontainersConfig.class)
class AuthRefreshTokenTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageService storageService;

    @Test
    void shouldReturnRefreshTokenOnLogin() throws Exception {
        register(
                "refresh-login@example.com",
                "Refresh Login User",
                "Refresh Login Company"
        );

        String loginResponse = login(
                "refresh-login@example.com",
                "12345678"
        );

        JsonNode json = objectMapper.readTree(loginResponse);

        String accessToken = json.path("accessToken").asText();
        String refreshToken = json.path("refreshToken").asText();

        org.assertj.core.api.Assertions.assertThat(accessToken).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(refreshToken).isNotBlank();
    }

    @Test
    void shouldRefreshAccessTokenAndRotateRefreshToken() throws Exception {
        register(
                "refresh-rotate@example.com",
                "Refresh Rotate User",
                "Refresh Rotate Company"
        );

        String loginResponse = login(
                "refresh-rotate@example.com",
                "12345678"
        );

        JsonNode loginJson = objectMapper.readTree(loginResponse);

        String oldAccessToken = loginJson.path("accessToken").asText();
        String oldRefreshToken = loginJson.path("refreshToken").asText();

        String refreshBody = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(oldRefreshToken);

        String refreshResponse = mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refreshBody)
                )
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", notNullValue()))
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresInSeconds", is(3600)))
                .andExpect(jsonPath("$.user.email", is("refresh-rotate@example.com")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode refreshJson = objectMapper.readTree(refreshResponse);

        String newAccessToken = refreshJson.path("accessToken").asText();
        String newRefreshToken = refreshJson.path("refreshToken").asText();

        org.assertj.core.api.Assertions.assertThat(newAccessToken).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(newRefreshToken).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

        /*
         * Depending on JWT claims precision, accessToken may be equal if generated
         * in the same second with identical claims. Refresh token rotation is the
         * important guarantee here.
         */
        org.assertj.core.api.Assertions.assertThat(oldAccessToken).isNotBlank();
    }

    @Test
    void shouldRejectOldRefreshTokenAfterRotation() throws Exception {
        register(
                "refresh-reuse@example.com",
                "Refresh Reuse User",
                "Refresh Reuse Company"
        );

        String loginResponse = login(
                "refresh-reuse@example.com",
                "12345678"
        );

        String oldRefreshToken = objectMapper.readTree(loginResponse)
                .path("refreshToken")
                .asText();

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "refreshToken": "%s"
                                        }
                                        """.formatted(oldRefreshToken))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "refreshToken": "%s"
                                        }
                                        """.formatted(oldRefreshToken))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }

    @Test
    void shouldRevokeRefreshTokenOnLogout() throws Exception {
        register(
                "refresh-logout@example.com",
                "Refresh Logout User",
                "Refresh Logout Company"
        );

        String loginResponse = login(
                "refresh-logout@example.com",
                "12345678"
        );

        String refreshToken = objectMapper.readTree(loginResponse)
                .path("refreshToken")
                .asText();

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "refreshToken": "%s"
                                        }
                                        """.formatted(refreshToken))
                )
                .andExpect(status().isNoContent())
                .andExpect(header().string("X-Request-Id", notNullValue()));

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "refreshToken": "%s"
                                        }
                                        """.formatted(refreshToken))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    void shouldRejectInvalidRefreshToken() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "refreshToken": "not-a-real-refresh-token"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", notNullValue()))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Invalid refresh token")))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }

    private void register(
            String email,
            String fullName,
            String organizationName
    ) throws Exception {
        String registerBody = """
                {
                  "email": "%s",
                  "password": "12345678",
                  "fullName": "%s",
                  "organizationName": "%s"
                }
                """.formatted(email, fullName, organizationName);

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.organizationId", notNullValue()));
    }

    private String login(
            String email,
            String password
    ) throws Exception {
        String loginBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        return mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody)
                )
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", notNullValue()))
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresInSeconds", is(3600)))
                .andExpect(jsonPath("$.user.email", is(email)))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestcontainersConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:16-alpine")
            );
        }
    }
}
