package com.complipilot.backend.task;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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
@Import(ComplianceTaskControllerTest.TestcontainersConfig.class)
class ComplianceTaskControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageService storageService;

    @Test
    void shouldCreateListUpdateAndCompleteComplianceTask() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "task-crud@example.com",
                "Task Crud User",
                "Task Crud Company"
        );

        String dueDate = LocalDate.now().plusDays(10).toString();

        String taskId = createTask(
                workspace.accessToken(),
                workspace.organizationId(),
                workspace.firstComplianceItemId(),
                "Upload MFA evidence",
                "Upload MFA screenshot or policy.",
                "HIGH",
                dueDate
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/tasks", workspace.organizationId())
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(taskId)))
                .andExpect(jsonPath("$.items[0].title", is("Upload MFA evidence")))
                .andExpect(jsonPath("$.items[0].status", is("OPEN")))
                .andExpect(jsonPath("$.items[0].priority", is("HIGH")))
                .andExpect(jsonPath("$.items[0].dueDate", is(dueDate)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalItems", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));

        String updateBody = """
                {
                  "title": "Upload MFA evidence",
                  "description": "Work started.",
                  "assigneeUserId": null,
                  "priority": "HIGH",
                  "dueDate": "%s",
                  "status": "IN_PROGRESS"
                }
                """.formatted(dueDate);

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/tasks/{taskId}",
                                workspace.organizationId(),
                                taskId
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.completedAt").doesNotExist());

        String completeBody = """
                {
                  "title": "Upload MFA evidence",
                  "description": "Evidence uploaded.",
                  "assigneeUserId": null,
                  "priority": "HIGH",
                  "dueDate": "%s",
                  "status": "DONE"
                }
                """.formatted(dueDate);

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/tasks/{taskId}",
                                workspace.organizationId(),
                                taskId
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(completeBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")))
                .andExpect(jsonPath("$.completedAt", notNullValue()));
    }

    @Test
    void shouldReturnComplianceTaskSummary() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "task-summary@example.com",
                "Task Summary User",
                "Task Summary Company"
        );

        String dueDate = LocalDate.now().plusDays(5).toString();

        String taskId = createTask(
                workspace.accessToken(),
                workspace.organizationId(),
                workspace.firstComplianceItemId(),
                "Review MFA control",
                "Review MFA compliance status.",
                "MEDIUM",
                dueDate
        );

        mockMvc.perform(
                        patch("/api/v1/organizations/{organizationId}/tasks/{taskId}",
                                workspace.organizationId(),
                                taskId
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Review MFA control",
                                          "description": "Review started.",
                                          "assigneeUserId": null,
                                          "priority": "MEDIUM",
                                          "dueDate": "%s",
                                          "status": "IN_PROGRESS"
                                        }
                                        """.formatted(dueDate))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/tasks/summary", workspace.organizationId())
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId", is(workspace.organizationId())))
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.open", is(0)))
                .andExpect(jsonPath("$.inProgress", is(1)))
                .andExpect(jsonPath("$.done", is(0)))
                .andExpect(jsonPath("$.cancelled", is(0)))
                .andExpect(jsonPath("$.overdue", is(0)));
    }

    @Test
    void shouldCountOverdueComplianceTasks() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "task-overdue@example.com",
                "Task Overdue User",
                "Task Overdue Company"
        );

        String overdueDate = LocalDate.now().minusDays(1).toString();

        createTask(
                workspace.accessToken(),
                workspace.organizationId(),
                workspace.firstComplianceItemId(),
                "Overdue task",
                "This task is overdue.",
                "CRITICAL",
                overdueDate
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/tasks/summary", workspace.organizationId())
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.open", is(1)))
                .andExpect(jsonPath("$.overdue", is(1)));
    }

    @Test
    void shouldDeleteComplianceTask() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "task-delete@example.com",
                "Task Delete User",
                "Task Delete Company"
        );

        String taskId = createTask(
                workspace.accessToken(),
                workspace.organizationId(),
                workspace.firstComplianceItemId(),
                "Delete me",
                "This task will be deleted.",
                "LOW",
                LocalDate.now().plusDays(3).toString()
        );

        mockMvc.perform(
                        delete("/api/v1/organizations/{organizationId}/tasks/{taskId}",
                                workspace.organizationId(),
                                taskId
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/tasks", workspace.organizationId())
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
    void shouldReturnPaginatedComplianceTasks() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "task-page@example.com",
                "Task Page User",
                "Task Page Company"
        );

        createTask(
                workspace.accessToken(),
                workspace.organizationId(),
                workspace.firstComplianceItemId(),
                "Task A",
                "First task.",
                "LOW",
                LocalDate.now().plusDays(1).toString()
        );

        createTask(
                workspace.accessToken(),
                workspace.organizationId(),
                workspace.firstComplianceItemId(),
                "Task B",
                "Second task.",
                "MEDIUM",
                LocalDate.now().plusDays(2).toString()
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/tasks?page=0&size=1",
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
    void shouldFilterComplianceTasksByStatusAndPriority() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "task-filter@example.com",
                "Task Filter User",
                "Task Filter Company"
        );

        String highTaskId = createTask(
                workspace.accessToken(),
                workspace.organizationId(),
                workspace.firstComplianceItemId(),
                "High task",
                "High priority task.",
                "HIGH",
                LocalDate.now().plusDays(1).toString()
        );

        createTask(
                workspace.accessToken(),
                workspace.organizationId(),
                workspace.firstComplianceItemId(),
                "Low task",
                "Low priority task.",
                "LOW",
                LocalDate.now().plusDays(2).toString()
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/tasks?status=OPEN&priority=HIGH&page=0&size=20",
                                workspace.organizationId()
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", is(1)))
                .andExpect(jsonPath("$.items[0].id", is(highTaskId)))
                .andExpect(jsonPath("$.items[0].title", is("High task")))
                .andExpect(jsonPath("$.items[0].status", is("OPEN")))
                .andExpect(jsonPath("$.items[0].priority", is("HIGH")))
                .andExpect(jsonPath("$.totalItems", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }



    @Test
    void shouldPreventTaskAccessAcrossOrganizations() throws Exception {
        TestWorkspace ownerA = createWorkspaceWithAppliedFramework(
                "task-owner-a@example.com",
                "Task Owner A",
                "Task Owner A Company"
        );

        TestWorkspace ownerB = createWorkspaceWithAppliedFramework(
                "task-owner-b@example.com",
                "Task Owner B",
                "Task Owner B Company"
        );

        createTask(
                ownerA.accessToken(),
                ownerA.organizationId(),
                ownerA.firstComplianceItemId(),
                "Tenant A task",
                "Private task.",
                "HIGH",
                LocalDate.now().plusDays(3).toString()
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/tasks", ownerA.organizationId())
                                .header("Authorization", "Bearer " + ownerB.accessToken())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You do not have access to this organization")));
    }

    @Test
    void shouldRejectTaskApisWithoutAccessToken() throws Exception {
        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/tasks",
                                "00000000-0000-0000-0000-000000000000"
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFilterComplianceTasksByComplianceItemId() throws Exception {
        TestWorkspace workspace = createWorkspaceWithAppliedFramework(
                "task-filter-item@example.com",
                "Task Filter Item User",
                "Task Filter Item Company"
        );

        String taskId = createTask(
                workspace.accessToken(),
                workspace.organizationId(),
                workspace.firstComplianceItemId(),
                "Compliance item task",
                "Task attached to compliance item.",
                "MEDIUM",
                LocalDate.now().plusDays(1).toString()
        );

        mockMvc.perform(
                        get("/api/v1/organizations/{organizationId}/tasks?complianceItemId={complianceItemId}&page=0&size=20",
                                workspace.organizationId(),
                                workspace.firstComplianceItemId()
                        )
                                .header("Authorization", "Bearer " + workspace.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", is(1)))
                .andExpect(jsonPath("$.items[0].id", is(taskId)))
                .andExpect(jsonPath("$.items[0].complianceItemId", is(workspace.firstComplianceItemId())))
                .andExpect(jsonPath("$.totalItems", is(1)));
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

    private String createTask(
            String accessToken,
            String organizationId,
            String complianceItemId,
            String title,
            String description,
            String priority,
            String dueDate
    ) throws Exception {
        String body = """
                {
                  "complianceItemId": "%s",
                  "title": "%s",
                  "description": "%s",
                  "assigneeUserId": null,
                  "priority": "%s",
                  "dueDate": "%s"
                }
                """.formatted(
                complianceItemId,
                title,
                description,
                priority,
                dueDate
        );

        String response = mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/tasks", organizationId)
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
