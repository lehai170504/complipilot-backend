package com.complipilot.backend.common.ratelimit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complipilot.backend.common.storage.StorageService;

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

@SpringBootTest(
        properties = {
                "app.rate-limit.enabled=true",
                "app.rate-limit.auth-capacity=2",
                "app.rate-limit.auth-window-seconds=60"
        }
)
@AutoConfigureMockMvc
@Import(AuthRateLimitFilterTest.TestcontainersConfig.class)
class AuthRateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageService storageService;

    @Test
    void shouldRateLimitAuthLoginEndpoint() throws Exception {
        String loginBody = """
                {
                  "email": "missing@example.com",
                  "password": "wrong-password"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .header("X-Forwarded-For", "203.0.113.10")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "1"))
                .andExpect(header().string("X-Request-Id", notNullValue()));

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .header("X-Forwarded-For", "203.0.113.10")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "0"));

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .header("X-Forwarded-For", "203.0.113.10")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody)
                )
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().string("Retry-After", notNullValue()));
    }

    @Test
    void shouldNotRateLimitNonAuthEndpoint() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                            post("/api/v1/compliance/frameworks/seed/security-baseline")
                                    .header("X-Forwarded-For", "203.0.113.20")
                    )
                    .andExpect(status().isUnauthorized());
        }
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