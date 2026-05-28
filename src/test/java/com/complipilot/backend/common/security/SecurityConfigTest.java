package com.complipilot.backend.common.security;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complipilot.backend.common.storage.StorageService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageService storageService;

    @Test
    void shouldAllowPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", notNullValue()));
    }

    @Test
    void shouldAllowActuatorHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", notNullValue()));
    }

    @Test
    void shouldAllowActuatorInfoEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", notNullValue()));
    }

    @Test
    void shouldAllowRegisterEndpoint() throws Exception {
        String body = """
                {
                  "email": "security-public-register@example.com",
                  "password": "12345678",
                  "fullName": "Security Public Register",
                  "organizationName": "Security Public Company"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Request-Id", notNullValue()));
    }

    @Test
    void shouldAllowLoginEndpointWithoutToken() throws Exception {
        String registerBody = """
                {
                  "email": "security-public-login@example.com",
                  "password": "12345678",
                  "fullName": "Security Public Login",
                  "organizationName": "Security Login Company"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody)
                )
                .andExpect(status().isCreated());

        String loginBody = """
                {
                  "email": "security-public-login@example.com",
                  "password": "12345678"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody)
                )
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", notNullValue()));
    }

    @Test
    void shouldProtectCurrentUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", notNullValue()));
    }

    @Test
    void shouldProtectBusinessApiEndpoint() throws Exception {
        mockMvc.perform(
                        get("/api/v1/organizations/00000000-0000-0000-0000-000000000000/compliance-items")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", notNullValue()));
    }
}
