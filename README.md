# CompliPilot Backend

CompliPilot Backend is the Spring Boot API for **CompliPilot — AI Compliance & Evidence OS**.

The backend provides authentication, organization tenancy, compliance framework management, evidence management, audit trail, compliance task tracking, observability, and production-oriented security foundations.

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
* Caffeine cache

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

Completed backend capabilities:

```txt
Backend foundation
Authentication
JWT access token security
Refresh token rotation
Organizations / tenancy
Compliance frameworks
Compliance items
Due soon / overdue compliance views
Evidence metadata
MinIO presigned upload/download URLs
Evidence-to-control linking
Audit trail
Compliance tasks
Request ID / correlation logging
CORS hardening
Auth endpoint rate limiting
Actuator health/info
Dockerfile
Production profile
Production compose
GitHub Actions CI
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
JWT_REFRESH_TOKEN_EXPIRATION_SECONDS=2592000
JWT_REFRESH_TOKEN_CLEANUP_FIXED_RATE_MS=3600000
JWT_REVOKED_REFRESH_TOKEN_RETENTION_SECONDS=604800

# Rate limit
RATE_LIMIT_ENABLED=true
RATE_LIMIT_AUTH_CAPACITY=20
RATE_LIMIT_AUTH_WINDOW_SECONDS=60

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

Actuator health:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

Actuator info:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/info | ConvertTo-Json -Depth 10
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

Rate limit is disabled by default for most tests in:

```txt
src/test/resources/application.yml
```

Only dedicated rate limit tests should enable it explicitly.

If tests fail with:

```txt
ApplicationContext failure threshold exceeded
```

that is usually not the real root cause. Check the first error in:

```txt
target/surefire-reports
```

Common fixes:

```txt
1. Add TestcontainersConfig for PostgreSQL.
2. Mock StorageService using @MockitoBean.
3. Register JavaTimeModule for ObjectMapper.
4. Check repository method names for nested JPA fields.
5. Disable rate limit for broad integration tests.
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
  -e JWT_SECRET=dev-prod-local-run-secret-9cfc4f5b47b54b3c9e6b0db3a0d4d2b8 `
  -e JWT_ISSUER=complipilot-backend `
  -e JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=3600 `
  -e JWT_REFRESH_TOKEN_EXPIRATION_SECONDS=2592000 `
  -e JWT_REFRESH_TOKEN_CLEANUP_FIXED_RATE_MS=3600000 `
  -e JWT_REVOKED_REFRESH_TOKEN_RETENTION_SECONDS=604800 `
  -e RATE_LIMIT_ENABLED=true `
  -e RATE_LIMIT_AUTH_CAPACITY=20 `
  -e RATE_LIMIT_AUTH_WINDOW_SECONDS=60 `
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
API_PORT=8082
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
JWT_SECRET=dev-prod-local-run-secret-9cfc4f5b47b54b3c9e6b0db3a0d4d2b8
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
Invoke-RestMethod http://localhost:8082/api/v1/health
```

Actuator info:

```powershell
Invoke-RestMethod http://localhost:8082/actuator/info | ConvertTo-Json -Depth 10
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
GET  /actuator/info
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
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

Register creates:

```txt
User
Organization
OWNER membership
```

Register does not automatically login. Frontend should call login after registration.

---

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
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "user": {
    "id": "...",
    "email": "hai@example.com",
    "fullName": "Lê Hoàng Hải"
  }
}
```

Access token is used in:

```http
Authorization: Bearer <accessToken>
```

Refresh token is used to obtain a new access token.

---

### Refresh Token Flow

```http
POST /api/v1/auth/refresh
```

Request:

```json
{
  "refreshToken": "<refreshToken>"
}
```

Response:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "user": {
    "id": "...",
    "email": "hai@example.com",
    "fullName": "Lê Hoàng Hải"
  }
}
```

Important:

```txt
Refresh token rotation is enabled.
After refresh succeeds, the old refresh token is revoked.
Frontend must store the new refresh token returned by the backend.
Backend stores only refresh token hashes.
```

Invalid, expired, revoked, or reused refresh token returns:

```http
401 Unauthorized
```

---

### Logout

```http
POST /api/v1/auth/logout
```

Request:

```json
{
  "refreshToken": "<refreshToken>"
}
```

Response:

```txt
204 No Content
```

Logout revokes the submitted refresh token. Frontend should clear local auth state after logout.

---

### Current User

```http
GET /api/v1/me
```

### Current User Organizations

```http
GET /api/v1/me/organizations
```

---

## Recommended Frontend Auth Flow

### Login flow

```txt
1. POST /api/v1/auth/login
2. Store accessToken and refreshToken
3. GET /api/v1/me
4. GET /api/v1/me/organizations
5. Set active organization
```

### Refresh flow

```txt
1. API request returns 401 Unauthorized
2. Frontend calls POST /api/v1/auth/refresh with stored refreshToken
3. If refresh succeeds:
   - Store new accessToken
   - Store new refreshToken
   - Retry the original request once
4. If refresh fails:
   - Clear auth state
   - Redirect user to login
```

### Logout flow

```txt
1. POST /api/v1/auth/logout with current refreshToken
2. Clear accessToken, refreshToken, user, and active organization from frontend state
3. Redirect to login
```

Frontend must not retry refresh endlessly. Retry the failed request once only.

---

## Refresh Token Cleanup

Refresh tokens are stored as hashes in the database.

Cleanup job deletes:

```txt
expired refresh tokens
revoked refresh tokens older than retention window
```

Config:

```env
JWT_REFRESH_TOKEN_CLEANUP_FIXED_RATE_MS=3600000
JWT_REVOKED_REFRESH_TOKEN_RETENTION_SECONDS=604800
```

Defaults:

```txt
Cleanup every 1 hour
Keep revoked tokens for 7 days
```

For MVP, cleanup runs inside the backend app instance.

For multi-instance production deployment, this can later be improved with:

```txt
distributed lock
dedicated scheduler
database scheduled job
```

---

## Rate Limiting

Auth endpoints are rate limited to reduce brute-force attempts.

Rate-limited endpoints:

```txt
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
```

Local defaults:

```env
RATE_LIMIT_ENABLED=true
RATE_LIMIT_AUTH_CAPACITY=20
RATE_LIMIT_AUTH_WINDOW_SECONDS=60
```

Production defaults:

```env
RATE_LIMIT_ENABLED=true
RATE_LIMIT_AUTH_CAPACITY=10
RATE_LIMIT_AUTH_WINDOW_SECONDS=60
```

When the limit is exceeded, backend returns:

```http
429 Too Many Requests
```

With headers:

```http
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 2026-05-28T...
Retry-After: 60
```

And body:

```json
{
  "timestamp": "2026-05-28T...",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Too many authentication requests. Please try again later.",
  "path": "/api/v1/auth/login",
  "requestId": "...",
  "fieldViolations": []
}
```

For tests, rate limit is disabled by default in:

```txt
src/test/resources/application.yml
```

Only dedicated rate limit tests enable it.

---

## Request ID / Observability

Backend returns `X-Request-Id` on every response.

If frontend sends:

```http
X-Request-Id: frontend-request-id
```

Backend echoes the same value.

If frontend does not send it, backend generates a UUID.

Error responses include the same request id:

```json
{
  "timestamp": "2026-05-28T...",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication is required",
  "path": "/api/v1/me",
  "requestId": "8f8a3c5d-...",
  "fieldViolations": []
}
```

Logs also include request id through MDC.

This makes production debugging easier:

```txt
User reports error requestId
Developer searches logs by requestId
```

---

## CORS

Local frontend origin:

```txt
http://localhost:3000
```

Backend env:

```env
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
```

Production example:

```env
APP_CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
```

Do not use `*` in production.

Backend allows these request headers from configured origins:

```txt
Authorization
Content-Type
X-Request-Id
```

Backend exposes this response header:

```txt
X-Request-Id
```

Manual preflight test:

```powershell
curl.exe -i -X OPTIONS http://localhost:8081/api/v1/auth/login `
  -H "Origin: http://localhost:3000" `
  -H "Access-Control-Request-Method: POST"
```

Expected:

```txt
Access-Control-Allow-Origin: http://localhost:3000
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

Due soon rules:

```txt
dueDate is from today through today + 14 days
status is not COMPLIANT or WAIVED
```

Overdue rules:

```txt
dueDate is before today
status is not COMPLIANT or WAIVED
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
netstat -ano | findstr :8082
```

Find process:

```powershell
tasklist /FI "PID eq <PID>"
```

Local recommendation:

```txt
Use backend port 8081 for Maven local dev.
Use backend port 8082 for production-like Docker if 8080 is occupied.
```

If `taskkill` fails with access denied, do not force kill system services. Change `API_PORT` instead.

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
5. Disable rate limit for broad integration tests.
```

---

### PowerShell does not support `-SkipHttpErrorCheck`

Older Windows PowerShell may not support:

```powershell
-SkipHttpErrorCheck
```

Use `curl.exe` instead:

```powershell
curl.exe -i http://localhost:8081/api/v1/me
```

or use try/catch:

```powershell
try {
  Invoke-WebRequest http://localhost:8081/api/v1/me
} catch {
  $_.Exception.Response.StatusCode.value__
  $_.Exception.Response.StatusDescription

  $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
  $reader.ReadToEnd()
}
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
* Configure `JWT_REFRESH_TOKEN_EXPIRATION_SECONDS`
* Configure refresh token cleanup schedule
* Configure auth rate limit values
* Set `APP_CORS_ALLOWED_ORIGINS` to real frontend domain
* Do not use `*` for production CORS
* Set `SPRINGDOC_ENABLED=false`
* Use persistent PostgreSQL volume or managed PostgreSQL
* Use private MinIO/S3 bucket
* Configure HTTPS at reverse proxy/load balancer
* Configure database backup
* Configure log collection
* Verify `X-Request-Id` appears in logs and error responses
* Verify `/actuator/health` works
* Verify `/actuator/info` works
* Run Flyway migrations on startup
* Run CI before deploy
* Never commit real `.env` files

---

## Frontend Contract

The latest frontend API contract file generated during development:

```txt
complipilot-fe-api-contract-v0.5.md
```

Frontend local Maven backend:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8081
```

Frontend production-like Docker backend:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8082
```

---

## Current Development Status

Completed:

```txt
Backend foundation
Authentication
JWT access token security
Refresh token rotation
Refresh token cleanup
Organizations / tenancy
Compliance frameworks
Compliance items
Due soon / overdue compliance views
Evidence metadata
MinIO presigned upload/download URLs
Audit trail
Compliance tasks
Request ID / observability
CORS hardening
Auth endpoint rate limiting
Actuator health/info
Dockerfile
Production profile
Production compose
GitHub Actions CI
```

Next recommended work:

```txt
Frontend implementation
API pagination/sorting for audit/tasks/evidence
Deployment setup
Advanced role management
Email invitations
Evidence OCR/AI extraction
Compliance report export
```
