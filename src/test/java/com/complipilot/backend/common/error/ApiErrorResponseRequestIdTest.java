package com.complipilot.backend.common.error;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complipilot.backend.common.storage.StorageService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiErrorResponseRequestIdTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageService storageService;

    @Test
    void shouldIncludeRequestIdInUnauthorizedErrorResponse() throws Exception {
        mockMvc.perform(
                        get("/api/v1/me")
                                .header("X-Request-Id", "frontend-error-request-id-123")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", "frontend-error-request-id-123"))
                .andExpect(jsonPath("$.requestId", is("frontend-error-request-id-123")))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }
}
