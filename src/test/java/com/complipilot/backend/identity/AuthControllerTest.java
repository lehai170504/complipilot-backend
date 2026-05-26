package com.complipilot.backend.identity;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complipilot.backend.organization.OrganizationMemberRepository;
import com.complipilot.backend.organization.OrganizationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthControllerTest.TestcontainersConfig.class)
class AuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository organizationMemberRepository;

    @Test
    void shouldRegisterUserAndCreateOwnerMembership() throws Exception {
        RegisterPayload payload = new RegisterPayload(
                "hai@example.com",
                "12345678",
                "Lê Hoàng Hải",
                "CompliPilot Demo Company"
        );

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.organizationId", notNullValue()))
                .andExpect(jsonPath("$.email", is("hai@example.com")))
                .andExpect(jsonPath("$.fullName", is("Lê Hoàng Hải")))
                .andExpect(jsonPath("$.organizationName", is("CompliPilot Demo Company")))
                .andExpect(jsonPath("$.role", is("OWNER")));

        User user = userRepository.findByEmailIgnoreCase("hai@example.com")
                .orElseThrow();

        assertFalse(user.getPasswordHash().isBlank());
        assertNotEquals("12345678", user.getPasswordHash());

        assert organizationRepository.existsBySlug("complipilot-demo-company");
        assert organizationMemberRepository.findByUserId(user.getId()).size() == 1;
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        RegisterPayload payload = new RegisterPayload(
                "duplicate@example.com",
                "12345678",
                "Duplicate User",
                "Duplicate Company"
        );

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload))
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Email is already registered")));
    }

    record RegisterPayload(
            String email,
            String password,
            String fullName,
            String organizationName
    ) {
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