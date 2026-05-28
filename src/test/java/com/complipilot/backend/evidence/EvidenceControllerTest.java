package com.complipilot.backend.evidence;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complipilot.backend.common.storage.StorageService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(EvidenceControllerTest.TestcontainersConfig.class)
class EvidenceControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageService storageService;

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
                .andExpect(jsonPath("$.items[0].id", is(evidenceId)))
                .andExpect(jsonPath("$.items[0].title", is("MFA configuration guide")))
                .andExpect(jsonPath("$.items[0].evidenceType", is("PROCEDURE")))
                .andExpect(jsonPath("$.items[0].sourceType", is("URL")))
                .andExpect(jsonPath("$.items[0].status", is("ACTIVE")))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalItems", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));

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
                .andExpect(jsonPath("$.items.length()", is(0)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalItems", is(0)))
                .andExpect(jsonPath("$.totalPages", is(0)));
    }

    @Test
    void shouldReturnPaginatedEvidenceDocuments() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "evidence-page@example.com",
                "Evidence Page User",
                "Evidence Page Company"
        );

        createUrlEvidence(
                workspace.accessToken(),
                workspace.organizationId(),
                "Evidence A",
                "https://example.com/evidence-a"
        );

        createUrlEvidence(
                workspace.accessToken(),
                workspace.organizationId(),
                "Evidence B",
                "https://example.com/evidence-b"
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/evidence?page=0&size=1",
                                workspace.organizationId()
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", is(1)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.totalItems", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    @Test
    void shouldFilterEvidenceDocumentsByEvidenceTypeAndSourceType() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "evidence-filter@example.com",
                "Evidence Filter User",
                "Evidence Filter Company"
        );

        String procedureEvidenceId = createUrlEvidence(
                workspace.accessToken(),
                workspace.organizationId(),
                "Procedure evidence",
                "https://example.com/procedure-evidence"
        );

        createFileEvidence(
                workspace.accessToken(),
                workspace.organizationId(),
                "Policy file evidence",
                "organizations/%s/evidence/policy-file.pdf".formatted(workspace.organizationId())
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/evidence?evidenceType=PROCEDURE&sourceType=URL&page=0&size=20",
                                workspace.organizationId()
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", is(1)))
                .andExpect(jsonPath("$.items[0].id", is(procedureEvidenceId)))
                .andExpect(jsonPath("$.items[0].evidenceType", is("PROCEDURE")))
                .andExpect(jsonPath("$.items[0].sourceType", is("URL")))
                .andExpect(jsonPath("$.totalItems", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void shouldRejectInvalidEvidenceTypeFilter() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "evidence-invalid-filter@example.com",
                "Evidence Invalid Filter User",
                "Evidence Invalid Filter Company"
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/evidence?evidenceType=WRONG&page=0&size=20",
                                workspace.organizationId()
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid value 'WRONG' for query parameter 'evidenceType'")))
                .andExpect(jsonPath("$.requestId", notNullValue()));
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

    @Test
    void shouldCreatePresignedUploadUrl() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "upload-url@example.com",
                "Upload Url User",
                "Upload Url Company"
        );

        when(storageService.generateEvidenceObjectKey(
                eq(java.util.UUID.fromString(workspace.organizationId())),
                eq("mfa-policy.pdf")
        )).thenReturn(
                "organizations/%s/evidence/test-mfa-policy.pdf".formatted(workspace.organizationId())
        );

        when(storageService.createPresignedUploadUrl(
                eq("organizations/%s/evidence/test-mfa-policy.pdf".formatted(workspace.organizationId())),
                eq("application/pdf")
        )).thenReturn("http://localhost:9000/presigned-upload-url");

        String body = """
                {
                  "filename": "mfa-policy.pdf",
                  "contentType": "application/pdf"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/evidence/upload-url", workspace.organizationId())
                                .header("Authorization", "Bearer " + workspace.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectKey", is("organizations/%s/evidence/test-mfa-policy.pdf".formatted(workspace.organizationId()))))
                .andExpect(jsonPath("$.uploadUrl", is("http://localhost:9000/presigned-upload-url")))
                .andExpect(jsonPath("$.method", is("PUT")))
                .andExpect(jsonPath("$.expiresInMinutes", is(15)));
    }

    @Test
    void shouldCreatePresignedDownloadUrlForFileEvidence() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "download-url@example.com",
                "Download Url User",
                "Download Url Company"
        );

        String objectKey = "organizations/%s/evidence/test-download.pdf"
                .formatted(workspace.organizationId());

        String evidenceBody = """
                {
                  "title": "Downloadable PDF evidence",
                  "description": "File evidence for download URL test.",
                  "evidenceType": "POLICY",
                  "sourceType": "FILE",
                  "fileObjectKey": "%s",
                  "externalUrl": null,
                  "contentType": "application/pdf",
                  "fileSizeBytes": 12345
                }
                """.formatted(objectKey);

        String evidenceResponse = mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/evidence", workspace.organizationId())
                                .header("Authorization", "Bearer " + workspace.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(evidenceBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.sourceType", is("FILE")))
                .andExpect(jsonPath("$.fileObjectKey", is(objectKey)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String evidenceId = objectMapper.readTree(evidenceResponse)
                .path("id")
                .asText();

        when(storageService.createPresignedDownloadUrl(eq(objectKey)))
                .thenReturn("http://localhost:9000/presigned-download-url");

        mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/evidence/{evidenceId}/download-url",
                                workspace.organizationId(),
                                evidenceId
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl", is("http://localhost:9000/presigned-download-url")))
                .andExpect(jsonPath("$.method", is("GET")))
                .andExpect(jsonPath("$.expiresInMinutes", is(15)));
    }

    @Test
    void shouldRejectDownloadUrlForUrlEvidence() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "download-url-reject@example.com",
                "Download Url Reject User",
                "Download Url Reject Company"
        );

        String evidenceId = createUrlEvidence(
                workspace.accessToken(),
                workspace.organizationId(),
                "URL evidence cannot be downloaded",
                "https://example.com/not-a-file"
        );

        mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/evidence/{evidenceId}/download-url",
                                workspace.organizationId(),
                                evidenceId
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Download URL is only available for file evidence")));
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
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse)
                .path("accessToken")
                .asText();

        return new AuthSession(accessToken, organizationId);
    }

    private String createFileEvidence(
            String accessToken,
            String organizationId,
            String title,
            String objectKey
    ) throws Exception {
        String body = """
            {
              "title": "%s",
              "description": "File evidence created during integration test.",
              "evidenceType": "POLICY",
              "sourceType": "FILE",
              "fileObjectKey": "%s",
              "externalUrl": null,
              "contentType": "application/pdf",
              "fileSizeBytes": 12345
            }
            """.formatted(title, objectKey);

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
