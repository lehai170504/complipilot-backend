package com.complipilot.backend.evidence;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class EvidenceControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateListUpdateAndArchiveEvidenceDocument() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "evidence-crud@example.com",
                "Evidence Crud User",
                "Evidence Crud Company"
        );

        String evidenceId = createUrlEvidence(
                workspace.accessToken(),
                workspace.organizationId(),
                "MFA configuration guide",
                "https://example.com/security/mfa-guide"
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/evidence", workspace.organizationId())
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(evidenceId)))
                .andExpect(jsonPath("$[0].title", is("MFA configuration guide")))
                .andExpect(jsonPath("$[0].evidenceType", is("PROCEDURE")))
                .andExpect(jsonPath("$[0].sourceType", is("URL")))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));

        String updateBody = """
                {
                  "title": "Updated MFA evidence",
                  "description": "Updated evidence description.",
                  "evidenceType": "POLICY",
                  "externalUrl": "https://example.com/security/updated-mfa-policy"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/evidence/{evidenceId}",
                                workspace.organizationId(),
                                evidenceId
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated MFA evidence")))
                .andExpect(jsonPath("$.description", is("Updated evidence description.")))
                .andExpect(jsonPath("$.evidenceType", is("POLICY")))
                .andExpect(jsonPath("$.externalUrl", is("https://example.com/security/updated-mfa-policy")));

        mockMvc.perform(
                        delete("/api/v1/organizations/{organizationId}/evidence/{evidenceId}",
                                workspace.organizationId(),
                                evidenceId
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/evidence", workspace.organizationId())
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void shouldLinkListAndUnlinkEvidenceFromComplianceItem() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "evidence-link@example.com",
                "Evidence Link User",
                "Evidence Link Company"
        );

        String evidenceId = createUrlEvidence(
                workspace.accessToken(),
                workspace.organizationId(),
                "MFA evidence link",
                "https://example.com/security/mfa-link"
        );

        mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/compliance-items/{itemId}/evidence-links",
                                workspace.organizationId(),
                                workspace.firstComplianceItemId()
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "evidenceDocumentId": "%s"
                                        }
                                        """.formatted(evidenceId))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.linkId", notNullValue()))
                .andExpect(jsonPath("$.complianceItemId", is(workspace.firstComplianceItemId())))
                .andExpect(jsonPath("$.evidence.id", is(evidenceId)))
                .andExpect(jsonPath("$.evidence.title", is("MFA evidence link")));

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/compliance-items/{itemId}/evidence-links",
                                workspace.organizationId(),
                                workspace.firstComplianceItemId()
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evidence.id", is(evidenceId)))
                .andExpect(jsonPath("$[0].evidence.sourceType", is("URL")));

        mockMvc.perform(
                        delete("/api/v1/organizations/{organizationId}/compliance-items/{itemId}/evidence-links/{evidenceDocumentId}",
                                workspace.organizationId(),
                                workspace.firstComplianceItemId(),
                                evidenceId
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/compliance-items/{itemId}/evidence-links",
                                workspace.organizationId(),
                                workspace.firstComplianceItemId()
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void shouldRejectDuplicateEvidenceLink() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "duplicate-evidence-link@example.com",
                "Duplicate Evidence Link User",
                "Duplicate Evidence Link Company"
        );

        String evidenceId = createUrlEvidence(
                workspace.accessToken(),
                workspace.organizationId(),
                "Duplicate evidence",
                "https://example.com/security/duplicate"
        );

        String body = """
                {
                  "evidenceDocumentId": "%s"
                }
                """.formatted(evidenceId);

        mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/compliance-items/{itemId}/evidence-links",
                                workspace.organizationId(),
                                workspace.firstComplianceItemId()
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/compliance-items/{itemId}/evidence-links",
                                workspace.organizationId(),
                                workspace.firstComplianceItemId()
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Evidence is already linked to this compliance item")));
    }

    @Test
    void shouldRejectUrlEvidenceWithoutExternalUrl() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "bad-url-evidence@example.com",
                "Bad Url Evidence User",
                "Bad Url Evidence Company"
        );

        String body = """
                {
                  "title": "Broken URL evidence",
                  "description": "This should fail.",
                  "evidenceType": "PROCEDURE",
                  "sourceType": "URL",
                  "fileObjectKey": null,
                  "externalUrl": null,
                  "contentType": null,
                  "fileSizeBytes": null
                }
                """;

        mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/evidence", workspace.organizationId())
                                .header("Authorization", "Bearer " + workspace.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("URL evidence requires externalUrl")));
    }

    @Test
    void shouldPreventEvidenceAccessAcrossOrganizations() throws Exception {
        TestWorkspace ownerA = createWorkspaceWithAppliedFramework(
                "evidence-owner-a@example.com",
                "Evidence Owner A",
                "Evidence Owner A Company"
        );

        TestWorkspace ownerB = createWorkspaceWithAppliedFramework(
                "evidence-owner-b@example.com",
                "Evidence Owner B",
                "Evidence Owner B Company"
        );

        createUrlEvidence(
                ownerA.accessToken(),
                ownerA.organizationId(),
                "Tenant A evidence",
                "https://example.com/tenant-a"
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/evidence", ownerA.organizationId())
                                .header("Authorization", "Bearer " + ownerB.accessToken())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You do not have access to this organization")));
    }

    @Test
    void shouldRejectEvidenceApisWithoutAccessToken() throws Exception {
        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/evidence",
                                "00000000-0000-0000-0000-000000000000"
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    private TestWorkspace createWorkspaceWithAppliedFramework(
            String email,
            String fullName,
            String organizationName
    ) throws Exception {
        AuthSession session = registerAndLogin(email, fullName, organizationName);

        String seedResponse = mockMvc.perform(
                        post("/api/v1/compliance/frameworks/seed/security-baseline")
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String frameworkId = objectMapper.readTree(seedResponse)
                .path("id")
                .asText();

        mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/compliance-frameworks/{frameworkId}/apply",
                                session.organizationId(),
                                frameworkId
                        )
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isCreated());

        String itemsResponse = mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/compliance-items", session.organizationId())
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(5)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstComplianceItemId = objectMapper.readTree(itemsResponse)
                .get(0)
                .path("id")
                .asText();

        return new TestWorkspace(
                session.accessToken(),
                session.organizationId(),
                firstComplianceItemId
        );
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

    private String createUrlEvidence(
            String accessToken,
            String organizationId,
            String title,
            String externalUrl
    ) throws Exception {
        String body = """
                {
                  "title": "%s",
                  "description": "Evidence created during integration test.",
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

    record AuthSession(
            String accessToken,
            String organizationId
    ) {
    }

    record TestWorkspace(
            String accessToken,
            String organizationId,
            String firstComplianceItemId
    ) {
    }

}
