package com.complipilot.backend.compliance;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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

import java.time.LocalDate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComplianceControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateFrameworkRequirementAndCompanyComplianceItem() throws Exception {
        AuthSession session = registerAndLogin(
                "compliance-flow@example.com",
                "Compliance Flow User",
                "Compliance Flow Company"
        );

        String frameworkId = createFramework(
                session.accessToken(),
                "FLOW-SEC",
                "Flow Security Framework"
        );

        String requirementId = createRequirement(
                session.accessToken(),
                frameworkId,
                "FLOW-001",
                "Enable MFA"
        );

        String itemId = createCompanyComplianceItem(
                session.accessToken(),
                session.organizationId(),
                requirementId
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/compliance-items", session.organizationId())
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(itemId)))
                .andExpect(jsonPath("$[0].organizationId", is(session.organizationId())))
                .andExpect(jsonPath("$[0].requirementId", is(requirementId)))
                .andExpect(jsonPath("$[0].requirementCode", is("FLOW-001")))
                .andExpect(jsonPath("$[0].requirementTitle", is("Enable MFA")))
                .andExpect(jsonPath("$[0].status", is("OPEN")));

        String updateBody = """
                {
                  "status": "IN_PROGRESS",
                  "ownerUserId": null,
                  "dueDate": "2026-06-30",
                  "notes": "Started MFA rollout."
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/compliance-items/{itemId}",
                                session.organizationId(),
                                itemId
                        )
                                .header("Authorization", "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.dueDate", is("2026-06-30")))
                .andExpect(jsonPath("$.notes", is("Started MFA rollout.")));
    }

    @Test
    void shouldRejectCreatingDuplicateCompanyComplianceItem() throws Exception {
        AuthSession session = registerAndLogin(
                "duplicate-item@example.com",
                "Duplicate Item User",
                "Duplicate Item Company"
        );

        String frameworkId = createFramework(
                session.accessToken(),
                "DUP-SEC",
                "Duplicate Framework"
        );

        String requirementId = createRequirement(
                session.accessToken(),
                frameworkId,
                "DUP-001",
                "Duplicate requirement"
        );

        createCompanyComplianceItem(
                session.accessToken(),
                session.organizationId(),
                requirementId
        );

        String body = """
                {
                  "requirementId": "%s"
                }
                """.formatted(requirementId);

        mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/compliance-items", session.organizationId())
                                .header("Authorization", "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Compliance item already exists for this organization and requirement")));
    }

    @Test
    void shouldPreventTenantDataAccessAcrossOrganizations() throws Exception {
        AuthSession ownerA = registerAndLogin(
                "tenant-a@example.com",
                "Tenant A Owner",
                "Tenant A Company"
        );

        AuthSession ownerB = registerAndLogin(
                "tenant-b@example.com",
                "Tenant B Owner",
                "Tenant B Company"
        );

        String frameworkId = createFramework(
                ownerA.accessToken(),
                "TENANT-SEC",
                "Tenant Security Framework"
        );

        String requirementId = createRequirement(
                ownerA.accessToken(),
                frameworkId,
                "TENANT-001",
                "Tenant isolated requirement"
        );

        createCompanyComplianceItem(
                ownerA.accessToken(),
                ownerA.organizationId(),
                requirementId
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/compliance-items", ownerA.organizationId())
                                .header("Authorization", "Bearer " + ownerB.accessToken())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You do not have access to this organization")));
    }

    @Test
    void shouldRejectComplianceItemsWithoutAccessToken() throws Exception {
        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/compliance-items",
                                "00000000-0000-0000-0000-000000000000"
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidComplianceStatusTransition() throws Exception {
        AuthSession session = registerAndLogin(
                "invalid-transition@example.com",
                "Invalid Transition User",
                "Invalid Transition Company"
        );

        String frameworkId = createFramework(
                session.accessToken(),
                "INVALID-TRANSITION",
                "Invalid Transition Framework"
        );

        String requirementId = createRequirement(
                session.accessToken(),
                frameworkId,
                "INVALID-001",
                "Invalid transition requirement"
        );

        String itemId = createCompanyComplianceItem(
                session.accessToken(),
                session.organizationId(),
                requirementId
        );

        String updateBody = """
                {
                  "status": "COMPLIANT",
                  "ownerUserId": null,
                  "dueDate": null,
                  "notes": "Trying to jump directly from OPEN to COMPLIANT."
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/compliance-items/{itemId}",
                                session.organizationId(),
                                itemId
                        )
                                .header("Authorization", "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid compliance status transition from OPEN to COMPLIANT")));
    }

    @Test
    void shouldAllowValidComplianceStatusTransitionToReadyForReview() throws Exception {
        AuthSession session = registerAndLogin(
                "valid-transition@example.com",
                "Valid Transition User",
                "Valid Transition Company"
        );

        String frameworkId = createFramework(
                session.accessToken(),
                "VALID-TRANSITION",
                "Valid Transition Framework"
        );

        String requirementId = createRequirement(
                session.accessToken(),
                frameworkId,
                "VALID-001",
                "Valid transition requirement"
        );

        String itemId = createCompanyComplianceItem(
                session.accessToken(),
                session.organizationId(),
                requirementId
        );

        String inProgressBody = """
                {
                  "status": "IN_PROGRESS",
                  "ownerUserId": null,
                  "dueDate": null,
                  "notes": "Implementation started."
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/compliance-items/{itemId}",
                                session.organizationId(),
                                itemId
                        )
                                .header("Authorization", "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(inProgressBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        String readyForReviewBody = """
                {
                  "status": "READY_FOR_REVIEW",
                  "ownerUserId": null,
                  "dueDate": null,
                  "notes": "Ready for compliance review."
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/compliance-items/{itemId}",
                                session.organizationId(),
                                itemId
                        )
                                .header("Authorization", "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(readyForReviewBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("READY_FOR_REVIEW")))
                .andExpect(jsonPath("$.notes", is("Ready for compliance review.")));
    }

    @Test
    void shouldSeedSecurityBaselineTemplate() throws Exception {
        AuthSession session = registerAndLogin(
                "seed-template@example.com",
                "Seed Template User",
                "Seed Template Company"
        );

        String seedResponse = mockMvc.perform(
                        post("/api/v1/compliance/frameworks/seed/security-baseline")
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.code", is("SME-SECURITY-BASELINE")))
                .andExpect(jsonPath("$.name", is("SME Security Baseline")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String frameworkId = objectMapper.readTree(seedResponse)
                .path("id")
                .asText();

        mockMvc.perform(
                        get("/api/v1/compliance/frameworks/{frameworkId}/requirements", frameworkId)
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code", is("SEC-001")))
                .andExpect(jsonPath("$[1].code", is("SEC-002")))
                .andExpect(jsonPath("$[2].code", is("SEC-003")))
                .andExpect(jsonPath("$[3].code", is("SEC-004")))
                .andExpect(jsonPath("$[4].code", is("SEC-005")));
    }

    @Test
    void shouldApplyFrameworkToOrganizationAndCreateComplianceItems() throws Exception {
        AuthSession session = registerAndLogin(
                "apply-framework@example.com",
                "Apply Framework User",
                "Apply Framework Company"
        );

        String seedResponse = mockMvc.perform(
                        post("/api/v1/compliance/frameworks/seed/security-baseline")
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationId", is(session.organizationId())))
                .andExpect(jsonPath("$.frameworkId", is(frameworkId)))
                .andExpect(jsonPath("$.createdCount", is(5)))
                .andExpect(jsonPath("$.skippedCount", is(0)))
                .andExpect(jsonPath("$.createdItems[0].status", is("OPEN")))
                .andExpect(jsonPath("$.createdItems[0].requirementCode", is("SEC-001")));

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/compliance-items", session.organizationId())
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(5)));
    }

    @Test
    void shouldSkipExistingItemsWhenApplyingFrameworkAgain() throws Exception {
        AuthSession session = registerAndLogin(
                "apply-framework-again@example.com",
                "Apply Again User",
                "Apply Again Company"
        );

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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdCount", is(5)))
                .andExpect(jsonPath("$.skippedCount", is(0)));

        mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/compliance-frameworks/{frameworkId}/apply",
                                session.organizationId(),
                                frameworkId
                        )
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdCount", is(0)))
                .andExpect(jsonPath("$.skippedCount", is(5)))
                .andExpect(jsonPath("$.createdItems.length()", is(0)));
    }

    @Test
    void shouldRejectApplyingFrameworkToAnotherOrganization() throws Exception {
        AuthSession ownerA = registerAndLogin(
                "apply-owner-a@example.com",
                "Apply Owner A",
                "Apply Owner A Company"
        );

        AuthSession ownerB = registerAndLogin(
                "apply-owner-b@example.com",
                "Apply Owner B",
                "Apply Owner B Company"
        );

        String seedResponse = mockMvc.perform(
                        post("/api/v1/compliance/frameworks/seed/security-baseline")
                                .header("Authorization", "Bearer " + ownerA.accessToken())
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
                                ownerA.organizationId(),
                                frameworkId
                        )
                                .header("Authorization", "Bearer " + ownerB.accessToken())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You do not have access to this organization")));
    }

    @Test
    void shouldReturnComplianceSummaryByStatus() throws Exception {
        AuthSession session = registerAndLogin(
                "summary@example.com",
                "Summary User",
                "Summary Company"
        );

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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdCount", is(5)));

        String itemsResponse = mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/compliance-items", session.organizationId())
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstItemId = objectMapper.readTree(itemsResponse)
                .get(0)
                .path("id")
                .asText();

        String updateBody = """
            {
              "status": "IN_PROGRESS",
              "ownerUserId": null,
              "dueDate": null,
              "notes": "Started first control."
            }
            """;

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/compliance-items/{itemId}",
                                session.organizationId(),
                                firstItemId
                        )
                                .header("Authorization", "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/compliance-summary", session.organizationId())
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId", is(session.organizationId())))
                .andExpect(jsonPath("$.totalItems", is(5)))
                .andExpect(jsonPath("$.open", is(4)))
                .andExpect(jsonPath("$.inProgress", is(1)))
                .andExpect(jsonPath("$.readyForReview", is(0)))
                .andExpect(jsonPath("$.compliant", is(0)))
                .andExpect(jsonPath("$.nonCompliant", is(0)))
                .andExpect(jsonPath("$.waived", is(0)));
    }

    @Test
    void shouldRejectComplianceSummaryForAnotherOrganization() throws Exception {
        AuthSession ownerA = registerAndLogin(
                "summary-owner-a@example.com",
                "Summary Owner A",
                "Summary Owner A Company"
        );

        AuthSession ownerB = registerAndLogin(
                "summary-owner-b@example.com",
                "Summary Owner B",
                "Summary Owner B Company"
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/compliance-summary", ownerA.organizationId())
                                .header("Authorization", "Bearer " + ownerB.accessToken())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You do not have access to this organization")));
    }

    @Test
    void shouldReturnDueSoonComplianceItems() throws Exception {
        AuthSession session = registerAndLogin(
                "due-soon@example.com",
                "Due Soon User",
                "Due Soon Company"
        );

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
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstItemId = objectMapper.readTree(itemsResponse)
                .get(0)
                .path("id")
                .asText();

        String dueDate = LocalDate.now().plusDays(7).toString();

        String updateBody = """
            {
              "status": "IN_PROGRESS",
              "ownerUserId": null,
              "dueDate": "%s",
              "notes": "Due soon item."
            }
            """.formatted(dueDate);

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/compliance-items/{itemId}",
                                session.organizationId(),
                                firstItemId
                        )
                                .header("Authorization", "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/compliance-items/due-soon", session.organizationId())
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].id", is(firstItemId)))
                .andExpect(jsonPath("$[0].dueDate", is(dueDate)))
                .andExpect(jsonPath("$[0].status", is("IN_PROGRESS")));
    }

    @Test
    void shouldReturnOverdueComplianceItems() throws Exception {
        AuthSession session = registerAndLogin(
                "overdue@example.com",
                "Overdue User",
                "Overdue Company"
        );

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
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstItemId = objectMapper.readTree(itemsResponse)
                .get(0)
                .path("id")
                .asText();

        String overdueDate = LocalDate.now().minusDays(1).toString();

        String updateBody = """
            {
              "status": "IN_PROGRESS",
              "ownerUserId": null,
              "dueDate": "%s",
              "notes": "Overdue item."
            }
            """.formatted(overdueDate);

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/compliance-items/{itemId}",
                                session.organizationId(),
                                firstItemId
                        )
                                .header("Authorization", "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/compliance-items/overdue", session.organizationId())
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].id", is(firstItemId)))
                .andExpect(jsonPath("$[0].dueDate", is(overdueDate)))
                .andExpect(jsonPath("$[0].status", is("IN_PROGRESS")));
    }

    @Test
    void shouldExcludeCompliantItemsFromOverdue() throws Exception {
        AuthSession session = registerAndLogin(
                "overdue-compliant@example.com",
                "Overdue Compliant User",
                "Overdue Compliant Company"
        );

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
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstItemId = objectMapper.readTree(itemsResponse)
                .get(0)
                .path("id")
                .asText();

        String overdueDate = LocalDate.now().minusDays(1).toString();

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/compliance-items/{itemId}",
                                session.organizationId(),
                                firstItemId
                        )
                                .header("Authorization", "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "status": "IN_PROGRESS",
                                      "ownerUserId": null,
                                      "dueDate": "%s",
                                      "notes": "Start item."
                                    }
                                    """.formatted(overdueDate))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/compliance-items/{itemId}",
                                session.organizationId(),
                                firstItemId
                        )
                                .header("Authorization", "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "status": "READY_FOR_REVIEW",
                                      "ownerUserId": null,
                                      "dueDate": "%s",
                                      "notes": "Ready."
                                    }
                                    """.formatted(overdueDate))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/compliance-items/{itemId}",
                                session.organizationId(),
                                firstItemId
                        )
                                .header("Authorization", "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "status": "COMPLIANT",
                                      "ownerUserId": null,
                                      "dueDate": "%s",
                                      "notes": "Compliant but old due date."
                                    }
                                    """.formatted(overdueDate))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/compliance-items/overdue", session.organizationId())
                                .header("Authorization", "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
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

        JsonNode registerJson = objectMapper.readTree(registerResponse);
        String organizationId = registerJson.path("organizationId").asText();

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

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String accessToken = loginJson.path("accessToken").asText();

        return new AuthSession(accessToken, organizationId);
    }

    private String createFramework(
            String accessToken,
            String code,
            String name
    ) throws Exception {
        String body = """
                {
                  "code": "%s",
                  "name": "%s",
                  "description": "Framework created during integration test."
                }
                """.formatted(code, name);

        String response = mockMvc.perform(
                        post("/api/v1/compliance/frameworks")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("id").asText();
    }

    private String createRequirement(
            String accessToken,
            String frameworkId,
            String code,
            String title
    ) throws Exception {
        String body = """
                {
                  "code": "%s",
                  "title": "%s",
                  "description": "Requirement created during integration test.",
                  "category": "Security",
                  "sortOrder": 1
                }
                """.formatted(code, title);

        String response = mockMvc.perform(
                        post("/api/v1/compliance/frameworks/{frameworkId}/requirements", frameworkId)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("id").asText();
    }

    private String createCompanyComplianceItem(
            String accessToken,
            String organizationId,
            String requirementId
    ) throws Exception {
        String body = """
                {
                  "requirementId": "%s"
                }
                """.formatted(requirementId);

        String response = mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/compliance-items", organizationId)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("id").asText();
    }

    record AuthSession(
            String accessToken,
            String organizationId
    ) {
    }
}
