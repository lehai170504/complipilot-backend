package com.complipilot.backend.audit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRecordAuditEventsForComplianceAndEvidenceActions() throws Exception {
        AuthSession session = registerAndLogin(
                "audit-flow@example.com",
                "Audit Flow User",
                "Audit Flow Company"
        );

        String frameworkId = seedSecurityBaseline(session.accessToken());

        applyFramework(
                session.accessToken(),
                session.organizationId(),
                frameworkId
        );

        String itemId = firstComplianceItemId(
                session.accessToken(),
                session.organizationId()
        );

        updateComplianceItemToInProgress(
                session.accessToken(),
                session.organizationId(),
                itemId
        );

        String evidenceId = createUrlEvidence(
                session.accessToken(),
                session.organizationId(),
                "Audit evidence URL",
                "https://example.com/audit-evidence"
        );

        linkEvidenceToComplianceItem(
                session.accessToken(),
                session.organizationId(),
                itemId,
                evidenceId
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/audit-events", session.organizationId())
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", notNullValue()))
                .andExpect(jsonPath("$.items[0].organizationId", is(session.organizationId())))
                .andExpect(jsonPath("$.items[0].actorUserId", notNullValue()))
                .andExpect(jsonPath("$.items[0].actorEmail", is("audit-flow@example.com")))
                .andExpect(jsonPath("$.items[*].action", hasItem("COMPLIANCE_FRAMEWORK_APPLIED")))
                .andExpect(jsonPath("$.items[*].action", hasItem("COMPLIANCE_ITEM_UPDATED")))
                .andExpect(jsonPath("$.items[*].action", hasItem("EVIDENCE_DOCUMENT_CREATED")))
                .andExpect(jsonPath("$.items[*].action", hasItem("EVIDENCE_LINK_CREATED")))
                .andExpect(jsonPath("$.items[*].summary", hasItem("Applied compliance framework to organization")))
                .andExpect(jsonPath("$.items[*].summary", hasItem("Updated compliance item")))
                .andExpect(jsonPath("$.items[*].summary", hasItem("Created evidence document")))
                .andExpect(jsonPath("$.items[*].summary", hasItem("Linked evidence to compliance item")));
    }

    @Test
    void shouldReturnRecentAuditEventsInDescendingOrder() throws Exception {
        AuthSession session = registerAndLogin(
                "audit-order@example.com",
                "Audit Order User",
                "Audit Order Company"
        );

        String frameworkId = seedSecurityBaseline(session.accessToken());

        applyFramework(
                session.accessToken(),
                session.organizationId(),
                frameworkId
        );

        String evidenceId = createUrlEvidence(
                session.accessToken(),
                session.organizationId(),
                "Audit ordering evidence",
                "https://example.com/audit-ordering"
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/audit-events", session.organizationId())
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", is(2)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalItems", is(2)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.items[0].action", is("EVIDENCE_DOCUMENT_CREATED")))
                .andExpect(jsonPath("$.items[0].resourceId", is(evidenceId)))
                .andExpect(jsonPath("$.items[1].action", is("COMPLIANCE_FRAMEWORK_APPLIED")));
    }

    @Test
    void shouldFilterAuditEventsByActionAndResourceType() throws Exception {
        AuthSession session = registerAndLogin(
                "audit-filter@example.com",
                "Audit Filter User",
                "Audit Filter Company"
        );

        String frameworkId = seedSecurityBaseline(session.accessToken());

        applyFramework(
                session.accessToken(),
                session.organizationId(),
                frameworkId
        );

        String evidenceId = createUrlEvidence(
                session.accessToken(),
                session.organizationId(),
                "Audit filter evidence",
                "https://example.com/audit-filter-evidence"
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/audit-events?action=EVIDENCE_DOCUMENT_CREATED&resourceType=EVIDENCE_DOCUMENT&page=0&size=20",
                                session.organizationId()
                        )
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", is(1)))
                .andExpect(jsonPath("$.items[0].action", is("EVIDENCE_DOCUMENT_CREATED")))
                .andExpect(jsonPath("$.items[0].resourceType", is("EVIDENCE_DOCUMENT")))
                .andExpect(jsonPath("$.items[0].resourceId", is(evidenceId)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalItems", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void shouldRejectInvalidAuditActionFilter() throws Exception {
        AuthSession session = registerAndLogin(
                "audit-invalid-filter@example.com",
                "Audit Invalid Filter User",
                "Audit Invalid Filter Company"
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/audit-events?action=WRONG&page=0&size=20",
                                session.organizationId()
                        )
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid value 'WRONG' for query parameter 'action'")))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }

    @Test
    void shouldPreventAuditAccessAcrossOrganizations() throws Exception {
        AuthSession ownerA = registerAndLogin(
                "audit-owner-a@example.com",
                "Audit Owner A",
                "Audit Owner A Company"
        );

        AuthSession ownerB = registerAndLogin(
                "audit-owner-b@example.com",
                "Audit Owner B",
                "Audit Owner B Company"
        );

        String frameworkId = seedSecurityBaseline(ownerA.accessToken());

        applyFramework(
                ownerA.accessToken(),
                ownerA.organizationId(),
                frameworkId
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/audit-events", ownerA.organizationId())
                                .header("Authorization", "Bearer " + ownerB.accessToken())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You do not have access to this organization")));
    }

    @Test
    void shouldRejectAuditEventsWithoutAccessToken() throws Exception {
        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/audit-events",
                                "00000000-0000-0000-0000-000000000000"
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    private AuthSession registerAndLogin(
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

        String registerResponse = mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationId", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String organizationId = objectMapper.readTree(registerResponse)
                .path("organizationId")
                .asText();

        String loginBody = """
                {
                  "email": "%s",
                  "password": "12345678"
                }
                """.formatted(email);

        String loginResponse = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse)
                .path("accessToken")
                .asText();

        return new AuthSession(accessToken, organizationId);
    }

    private String seedSecurityBaseline(String accessToken) throws Exception {
        String response = mockMvc.perform(
                        post("/api/v1/compliance/frameworks/seed/security-baseline")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .path("id")
                .asText();
    }

    private void applyFramework(
            String accessToken,
            String organizationId,
            String frameworkId
    ) throws Exception {
        mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/compliance-frameworks/{frameworkId}/apply",
                                organizationId,
                                frameworkId
                        )
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.frameworkId", is(frameworkId)));
    }

    private String firstComplianceItemId(
            String accessToken,
            String organizationId
    ) throws Exception {
        String itemsResponse = mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/compliance-items", organizationId)
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(5)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(itemsResponse)
                .get(0)
                .path("id")
                .asText();
    }

    private void updateComplianceItemToInProgress(
            String accessToken,
            String organizationId,
            String itemId
    ) throws Exception {
        String updateBody = """
                {
                  "status": "IN_PROGRESS",
                  "ownerUserId": null,
                  "dueDate": null,
                  "notes": "Audit test updated this compliance item."
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/compliance-items/{itemId}",
                                organizationId,
                                itemId
                        )
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    private String createUrlEvidence(
            String accessToken,
            String organizationId,
            String title,
            String externalUrl
    ) throws Exception {
        String body = """
                {
                  "title": "%s",
                  "description": "Evidence created during audit integration test.",
                  "evidenceType": "PROCEDURE",
                  "sourceType": "URL",
                  "fileObjectKey": null,
                  "externalUrl": "%s",
                  "contentType": null,
                  "fileSizeBytes": null
                }
                """.formatted(title, externalUrl);

        String response = mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/evidence", organizationId)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .path("id")
                .asText();
    }

    private void linkEvidenceToComplianceItem(
            String accessToken,
            String organizationId,
            String itemId,
            String evidenceId
    ) throws Exception {
        String body = """
                {
                  "evidenceDocumentId": "%s"
                }
                """.formatted(evidenceId);

        mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/compliance-items/{itemId}/evidence-links",
                                organizationId,
                                itemId
                        )
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evidence.id", is(evidenceId)));
    }

    record AuthSession(
            String accessToken,
            String organizationId
    ) {
    }
}
