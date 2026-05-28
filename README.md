# CompliPilot Backend

CompliPilot Backend is the Spring Boot API for **CompliPilot — AI Compliance & Evidence OS**.

The backend provides authentication, organization tenancy, compliance framework management, evidence management, audit trail, and compliance task tracking.

---

## Tech Stack

* Java 21
* Spring Boot 4
* Spring WebMVC
* Spring Security
* Spring Data JPA
* PostgreSQL
* Flyway
* Testcontainers
* MinIO
* Swagger / OpenAPI
* Maven Wrapper
* Docker / Docker Compose
* GitHub Actions CI

---

## Project Modules

Current backend modules:

```txt
Module A — Backend Foundation
Module B — Auth & Tenancy
Module C — Compliance Framework
Module D — Evidence Management
Module E — Audit Trail
Module F — Compliance Tasks / Deadline Views
Module H — Production Hardening
```

---

## Local Development Requirements

Install:

* Java 21
* Docker Desktop
* Git
* PowerShell
* IntelliJ IDEA or VS Code

Check tools:

```powershell
java -version
docker --version
docker compose version
git --version
```

Expected Java version should be Java 21.

---

## Environment Files

This project uses `.env` for local development.

### Local `.env`

Create local `.env`:

```powershell
Copy-Item .env.example .env
```

Recommended local `.env`:

```env
# App
API_PORT=8081
SPRING_PROFILES_ACTIVE=local
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000

# PostgreSQL
POSTGRES_DB=complipilot
POSTGRES_USER=complipilot
POSTGRES_PASSWORD=123456
POSTGRES_PORT=5433

# Spring datasource
DATABASE_URL=jdbc:postgresql://localhost:5433/complipilot
DATABASE_USERNAME=complipilot
DATABASE_PASSWORD=123456

# JWT
JWT_SECRET=local-dev-secret-key-change-this-in-production-please-123456
JWT_ISSUER=complipilot-backend
JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=3600

# MinIO
MINIO_ROOT_USER=complipilot
MINIO_ROOT_PASSWORD=complipilot_minio_password
MINIO_ENDPOINT=http://localhost:9000
MINIO_PUBLIC_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=complipilot
MINIO_SECRET_KEY=complipilot_minio_password
MINIO_BUCKET_EVIDENCE=complipilot-evidence
MINIO_PRESIGNED_URL_EXPIRATION_MINUTES=15
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001

# Swagger
SPRINGDOC_ENABLED=true
```

### Important Git Rule

Do commit:

```txt
.env.example
.env.prod.example
```

Do not commit:

```txt
.env
.env.prod
.env.prod.local
```

`.gitignore` should contain:

```gitignore
.env
.env.*
!.env.example
!.env.prod.example
```

---

## Local Service URLs

Default local URLs:

```txt
Backend API:      http://localhost:8081
Swagger UI:       http://localhost:8081/swagger-ui
OpenAPI JSON:     http://localhost:8081/v3/api-docs

PostgreSQL:       localhost:5433
MinIO API:        http://localhost:9000
MinIO Console:    http://localhost:9001
```

MinIO local credentials:

```txt
Username: complipilot
Password: complipilot_minio_password
```

---

## Start Local Infrastructure

From project root:

```powershell
cd D:\GitHub\complipilot-backend
docker compose up -d
docker compose ps
```

Expected containers:

```txt
complipilot-backend-postgres
complipilot-backend-minio
```

Check PostgreSQL:

```powershell
docker exec -it complipilot-backend-postgres psql -U complipilot -d complipilot
```

Inside `psql`:

```sql
SELECT current_user;
```

Exit:

```sql
\q
```

Open MinIO Console:

```txt
http://localhost:9001
```

---

## Run Backend Locally

Run app:

```powershell
cd D:\GitHub\complipilot-backend
.\mvnw.cmd spring-boot:run
```

Health check:

```powershell
Invoke-RestMethod http://localhost:8081/api/v1/health
```

Swagger:

```txt
http://localhost:8081/swagger-ui
```

---

## Run Tests

Run all tests:

```powershell
cd D:\GitHub\complipilot-backend
.\mvnw.cmd clean test
```

Run with stack trace:

```powershell
.\mvnw.cmd test -e
```

Run with debug logs:

```powershell
.\mvnw.cmd test -X
```

---

## Integration Test Pattern

Full-context controller tests should use:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Import(SomeControllerTest.TestcontainersConfig.class)
class SomeControllerTest {
}
```

Use PostgreSQL Testcontainers:

```java
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
```

Use local `ObjectMapper` with Java time support:

```java
private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());
```

If the test does not directly test MinIO or presigned URLs, mock storage:

```java
import com.complipilot.backend.common.storage.StorageService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@MockitoBean
private StorageService storageService;
```

Important Spring Boot 4 test note:

```txt
Use @MockitoBean from:
org.springframework.test.context.bean.override.mockito.MockitoBean

Do not use old @MockBean unless intentionally needed.
```

If tests fail with:

```txt
ApplicationContext failure threshold exceeded
```

that is usually not the real root cause. Check the first error in:

```txt
target/surefire-reports
```

---

## Build JAR

Build:

```powershell
.\mvnw.cmd clean package
```

Generated JAR:

```txt
target/*.jar
```

---

## Docker Build

Build backend Docker image:

```powershell
cd D:\GitHub\complipilot-backend
docker build -t complipilot-backend:local .
```

Important: run this command from project root where `Dockerfile` exists.

Check file name:

```powershell
dir | findstr Docker
```

The file must be named exactly:

```txt
Dockerfile
```

Not:

```txt
Dockerfile.txt
```

---

## Run Backend Container Locally

Start local infrastructure first:

```powershell
docker compose up -d
```

Run backend container:

```powershell
docker run --rm `
  --name complipilot-api `
  --network complipilot-backend_default `
  -p 8080:8080 `
  -e SPRING_PROFILES_ACTIVE=prod `
  -e API_PORT=8080 `
  -e DATABASE_URL=jdbc:postgresql://complipilot-backend-postgres:5432/complipilot `
  -e DATABASE_USERNAME=complipilot `
  -e DATABASE_PASSWORD=123456 `
  -e APP_CORS_ALLOWED_ORIGINS=http://localhost:3000 `
  -e JWT_SECRET=local-dev-secret-key-change-this-in-production-please-123456 `
  -e JWT_ISSUER=complipilot-backend `
  -e JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=3600 `
  -e MINIO_ENDPOINT=http://complipilot-backend-minio:9000 `
  -e MINIO_PUBLIC_ENDPOINT=http://localhost:9000 `
  -e MINIO_ACCESS_KEY=complipilot `
  -e MINIO_SECRET_KEY=complipilot_minio_password `
  -e MINIO_BUCKET_EVIDENCE=complipilot-evidence `
  -e MINIO_PRESIGNED_URL_EXPIRATION_MINUTES=15 `
  -e SPRINGDOC_ENABLED=true `
  complipilot-backend:local
```

Test container health:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/health
```

Swagger if enabled:

```txt
http://localhost:8080/swagger-ui
```

---

## Production-like Compose

Create local production env:

```powershell
Copy-Item .env.prod.example .env.prod.local
```

Edit:

```powershell
notepad .env.prod.local
```

Recommended local test values:

```env
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
JWT_SECRET=local-prod-test-secret-key-change-this-please-1234567890
MINIO_PUBLIC_ENDPOINT=http://localhost:9000
SPRINGDOC_ENABLED=true
```

Run production-like stack:

```powershell
docker compose --env-file .env.prod.local -f docker-compose.prod.yml up -d --build
```

Check services:

```powershell
docker compose --env-file .env.prod.local -f docker-compose.prod.yml ps
```

Health:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/health
```

Stop:

```powershell
docker compose --env-file .env.prod.local -f docker-compose.prod.yml down
```

Stop and remove volumes:

```powershell
docker compose --env-file .env.prod.local -f docker-compose.prod.yml down -v
```

---

## API Overview

### Public APIs

```http
GET  /api/v1/health
GET  /actuator/health
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /swagger-ui/**
GET  /v3/api-docs/**
```

All other APIs require:

```http
Authorization: Bearer <accessToken>
```

---

## Auth & Tenancy APIs

### Register

```http
POST /api/v1/auth/register
```

Request:

```json
{
  "email": "hai@example.com",
  "password": "12345678",
  "fullName": "Lê Hoàng Hải",
  "organizationName": "CompliPilot Demo Company"
}
```

### Login

```http
POST /api/v1/auth/login
```

Request:

```json
{
  "email": "hai@example.com",
  "password": "12345678"
}
```

Response contains:

```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "user": {
    "id": "...",
    "email": "hai@example.com",
    "fullName": "Lê Hoàng Hải"
  }
}
```

### Current User

```http
GET /api/v1/me
```

### Current User Organizations

```http
GET /api/v1/me/organizations
```

---

## Compliance APIs

### Frameworks

```http
POST /api/v1/compliance/frameworks/seed/security-baseline
GET  /api/v1/compliance/frameworks
POST /api/v1/compliance/frameworks
POST /api/v1/compliance/frameworks/{frameworkId}/requirements
GET  /api/v1/compliance/frameworks/{frameworkId}/requirements
```

Seed creates:

```txt
SME-SECURITY-BASELINE
```

With requirements:

```txt
SEC-001 Enable multi-factor authentication
SEC-002 Maintain user access review
SEC-003 Keep evidence for critical controls
SEC-004 Define incident response contact
SEC-005 Backup critical business data
```

### Organization Compliance

```http
POST  /api/v1/organizations/{organizationId}/compliance-frameworks/{frameworkId}/apply
GET   /api/v1/organizations/{organizationId}/compliance-items
GET   /api/v1/organizations/{organizationId}/compliance-items/due-soon
GET   /api/v1/organizations/{organizationId}/compliance-items/overdue
POST  /api/v1/organizations/{organizationId}/compliance-items
PATCH /api/v1/organizations/{organizationId}/compliance-items/{itemId}
GET   /api/v1/organizations/{organizationId}/compliance-summary
```

Compliance status values:

```txt
OPEN
IN_PROGRESS
READY_FOR_REVIEW
COMPLIANT
NON_COMPLIANT
WAIVED
```

Compliance status workflow:

```txt
OPEN
  -> IN_PROGRESS
  -> WAIVED

IN_PROGRESS
  -> READY_FOR_REVIEW
  -> WAIVED

READY_FOR_REVIEW
  -> IN_PROGRESS
  -> COMPLIANT
  -> NON_COMPLIANT
  -> WAIVED

NON_COMPLIANT
  -> IN_PROGRESS
  -> WAIVED

COMPLIANT
  -> IN_PROGRESS

WAIVED
  -> OPEN
```

---

## Evidence APIs

```http
POST   /api/v1/organizations/{organizationId}/evidence/upload-url
POST   /api/v1/organizations/{organizationId}/evidence
GET    /api/v1/organizations/{organizationId}/evidence
PATCH  /api/v1/organizations/{organizationId}/evidence/{evidenceId}
DELETE /api/v1/organizations/{organizationId}/evidence/{evidenceId}
POST   /api/v1/organizations/{organizationId}/evidence/{evidenceId}/download-url
POST   /api/v1/organizations/{organizationId}/compliance-items/{itemId}/evidence-links
GET    /api/v1/organizations/{organizationId}/compliance-items/{itemId}/evidence-links
DELETE /api/v1/organizations/{organizationId}/compliance-items/{itemId}/evidence-links/{evidenceDocumentId}
```

Evidence source types:

```txt
FILE
URL
TEXT_NOTE
```

Evidence types:

```txt
POLICY
PROCEDURE
SCREENSHOT
REPORT
CONTRACT
CERTIFICATE
AUDIT_NOTE
OTHER
```

File upload flow:

```txt
1. POST /api/v1/organizations/{organizationId}/evidence/upload-url
2. PUT file directly to returned uploadUrl
3. POST /api/v1/organizations/{organizationId}/evidence with sourceType FILE and fileObjectKey
4. Link evidence to compliance item if needed
```

---

## Task APIs

```http
POST   /api/v1/organizations/{organizationId}/tasks
GET    /api/v1/organizations/{organizationId}/tasks
GET    /api/v1/organizations/{organizationId}/tasks/summary
PATCH  /api/v1/organizations/{organizationId}/tasks/{taskId}
DELETE /api/v1/organizations/{organizationId}/tasks/{taskId}
```

Task statuses:

```txt
OPEN
IN_PROGRESS
DONE
CANCELLED
```

Task priorities:

```txt
LOW
MEDIUM
HIGH
CRITICAL
```

---

## Audit APIs

```http
GET /api/v1/organizations/{organizationId}/audit-events
```

Returns recent activity events for an organization.

Tracked events include:

```txt
COMPLIANCE_FRAMEWORK_APPLIED
COMPLIANCE_ITEM_CREATED
COMPLIANCE_ITEM_UPDATED
EVIDENCE_DOCUMENT_CREATED
EVIDENCE_DOCUMENT_UPDATED
EVIDENCE_DOCUMENT_ARCHIVED
EVIDENCE_LINK_CREATED
EVIDENCE_LINK_DELETED
COMPLIANCE_TASK_CREATED
COMPLIANCE_TASK_UPDATED
COMPLIANCE_TASK_DELETED
```

---

## Permission Rules

Active organization member can view:

```txt
Compliance items
Compliance summary
Due soon / overdue items
Evidence
Evidence links
Tasks
Task summary
Audit events
```

Roles that can manage compliance/evidence/tasks:

```txt
OWNER
ADMIN
COMPLIANCE_MANAGER
```

Cross-tenant access is forbidden.

A user from organization B cannot view or mutate organization A data.

---

## GitHub Actions CI

The project uses GitHub Actions workflow:

```txt
.github/workflows/ci.yml
```

CI should:

```txt
1. Checkout code
2. Setup Java 21
3. Run Maven tests
4. Build Docker image
```

Check workflow status in GitHub repository tab:

```txt
Actions
```

---

## Common Troubleshooting

### Docker command not found

Check:

```powershell
where.exe docker
docker --version
docker compose version
```

Make sure Docker Desktop is running.

---

### Dockerfile not found

Error:

```txt
failed to read dockerfile: open Dockerfile: no such file or directory
```

Fix:

```powershell
cd D:\GitHub\complipilot-backend
dir | findstr Docker
docker build -t complipilot-backend:local .
```

Make sure file name is exactly:

```txt
Dockerfile
```

not:

```txt
Dockerfile.txt
```

---

### Port already in use

Check port:

```powershell
netstat -ano | findstr :8080
netstat -ano | findstr :8081
```

Find process:

```powershell
tasklist /FI "PID eq <PID>"
```

Local recommendation:

```txt
Use backend port 8081 for Maven local dev.
Use backend port 8080 for production-like Docker.
```

---

### PostgreSQL connection failed

Check Docker:

```powershell
docker compose ps
```

Check PostgreSQL container:

```powershell
docker logs complipilot-backend-postgres
```

Check `.env`:

```env
POSTGRES_PORT=5433
DATABASE_URL=jdbc:postgresql://localhost:5433/complipilot
DATABASE_USERNAME=complipilot
DATABASE_PASSWORD=123456
```

---

### MinIO connection failed

Check Docker:

```powershell
docker compose ps
docker logs complipilot-backend-minio
```

Open console:

```txt
http://localhost:9001
```

Check `.env`:

```env
MINIO_ENDPOINT=http://localhost:9000
MINIO_PUBLIC_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=complipilot
MINIO_SECRET_KEY=complipilot_minio_password
MINIO_BUCKET_EVIDENCE=complipilot-evidence
```

---

### ApplicationContext failure in tests

If tests show:

```txt
ApplicationContext failure threshold exceeded
```

do not debug that repeated message first.

Open:

```txt
target/surefire-reports
```

Find first real root cause.

Common fixes:

```txt
1. Add TestcontainersConfig for PostgreSQL.
2. Mock StorageService using @MockitoBean.
3. Register JavaTimeModule for ObjectMapper.
4. Check repository method names for nested JPA fields.
```

---

## Useful Commands

Start local infra:

```powershell
docker compose up -d
```

Stop local infra:

```powershell
docker compose down
```

Stop and delete local volumes:

```powershell
docker compose down -v
```

Run tests:

```powershell
.\mvnw.cmd clean test
```

Run app:

```powershell
.\mvnw.cmd spring-boot:run
```

Build Docker image:

```powershell
docker build -t complipilot-backend:local .
```

Check Git status:

```powershell
git status
```

Commit:

```powershell
git add .
git commit -m "message"
git push
```

---

## Pre-Commit Checklist

Before pushing code:

```powershell
.\mvnw.cmd clean test
docker build -t complipilot-backend:local .
git status
```

Then commit:

```powershell
git add .
git commit -m "your message"
git push
```

---

## Production Checklist

Before real deployment:

* Use strong `POSTGRES_PASSWORD`
* Use strong `JWT_SECRET` with at least 64 random characters
* Set `APP_CORS_ALLOWED_ORIGINS` to real frontend domain
* Set `SPRINGDOC_ENABLED=false`
* Use persistent PostgreSQL volume or managed PostgreSQL
* Use private MinIO/S3 bucket
* Configure HTTPS at reverse proxy/load balancer
* Configure database backup
* Configure log collection
* Run Flyway migrations on startup
* Run CI before deploy
* Never commit real `.env` files

---

## Frontend Contract

The latest frontend API contract file generated during development:

```txt
complipilot-fe-api-contract-v0.3.md
```

Frontend should use:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8081
```

For Docker production-like local testing:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

---

## Current Development Status

Completed:

```txt
Backend foundation
Authentication
JWT security
Organizations / tenancy
Compliance frameworks
Compliance items
Evidence metadata
MinIO presigned upload/download URLs
Audit trail
Compliance tasks
Dockerfile
Production profile
Production compose
GitHub Actions CI
```

Next recommended work:

```txt
Env validation startup checks
Structured logging
Refresh tokens
Frontend implementation
Deployment setup
```
