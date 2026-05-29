# CompliPilot MVP Release Notes

## Release Name

CompliPilot MVP — AI Compliance & Evidence OS

## Release Scope

This MVP delivers an end-to-end compliance management system with backend APIs and frontend UI for:

```txt
Authentication
Organization workspace management
Compliance frameworks
Compliance controls
Evidence management
Evidence upload/download
Evidence linking
Compliance tasks
Audit trail
Dashboard overview
Production-like local Docker setup
Frontend SaaS workspace UI
```

The system is designed as a practical compliance operations platform where teams can track controls, upload evidence, assign tasks, and review audit history.

---

## Product Summary

CompliPilot is an **AI Compliance & Evidence OS** for managing compliance readiness in one workspace.

Core idea:

```txt
Controls define what must be satisfied.
Evidence proves how controls are satisfied.
Tasks coordinate work.
Audit events track what happened.
Dashboard summarizes readiness.
```

---

## Architecture Summary

The MVP consists of two main applications:

```txt
complipilot-backend
  Spring Boot REST API
  PostgreSQL database
  MinIO object storage
  JWT auth and refresh token rotation
  Audit trail
  Docker / CI support

complipilot-frontend
  Next.js App Router
  TypeScript
  Tailwind CSS
  shadcn/ui
  TanStack Query
  Cookie-based auth storage
  SaaS dashboard UI
```

Local service layout:

```txt
Frontend: http://localhost:3000
Backend:  http://localhost:8081
Postgres: localhost:5433
MinIO:    http://localhost:9000
MinIO UI: http://localhost:9001
```

---

## Backend Completed Scope

### Authentication

Implemented:

```txt
Register
Login
Refresh token
Logout
Current user endpoint
User organizations endpoint
JWT access token
Refresh token rotation
Refresh token revocation
Refresh token hash storage
```

Main endpoints:

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/me
GET  /api/v1/me/organizations
```

Security notes:

```txt
Access token is short-lived.
Refresh token rotation is enabled.
Logout revokes submitted refresh token.
Protected APIs require Bearer token.
```

---

### Organization and Tenant Access

Implemented:

```txt
Organization creation during register
Owner membership creation
Active member access checks
Role-based management checks
Cross-tenant access protection
```

Roles used:

```txt
OWNER
ADMIN
COMPLIANCE_MANAGER
MEMBER
AUDITOR
```

Management roles:

```txt
OWNER
ADMIN
COMPLIANCE_MANAGER
```

---

### Compliance Frameworks and Controls

Implemented:

```txt
Seed system security baseline framework
List compliance frameworks
Create framework
Create framework requirements
List framework requirements
Apply framework to organization
List company compliance items
Update compliance item status / owner / due date / notes
Compliance summary
Due soon controls
Overdue controls
```

Main endpoints:

```http
POST  /api/v1/compliance/frameworks/seed/security-baseline
GET   /api/v1/compliance/frameworks
POST  /api/v1/compliance/frameworks
POST  /api/v1/compliance/frameworks/{frameworkId}/requirements
GET   /api/v1/compliance/frameworks/{frameworkId}/requirements
POST  /api/v1/organizations/{organizationId}/compliance-frameworks/{frameworkId}/apply
GET   /api/v1/organizations/{organizationId}/compliance-items
POST  /api/v1/organizations/{organizationId}/compliance-items
PATCH /api/v1/organizations/{organizationId}/compliance-items/{itemId}
GET   /api/v1/organizations/{organizationId}/compliance-summary
GET   /api/v1/organizations/{organizationId}/compliance-items/due-soon
GET   /api/v1/organizations/{organizationId}/compliance-items/overdue
```

Compliance statuses:

```txt
OPEN
IN_PROGRESS
READY_FOR_REVIEW
COMPLIANT
NON_COMPLIANT
WAIVED
```

Status workflow:

```txt
OPEN -> IN_PROGRESS / WAIVED
IN_PROGRESS -> READY_FOR_REVIEW / WAIVED
READY_FOR_REVIEW -> IN_PROGRESS / COMPLIANT / NON_COMPLIANT / WAIVED
NON_COMPLIANT -> IN_PROGRESS / WAIVED
COMPLIANT -> IN_PROGRESS
WAIVED -> OPEN
```

---

### Evidence Management

Implemented:

```txt
Create evidence upload URL
Upload files through presigned URL
Create evidence metadata
Create URL evidence
List evidence
Filter evidence
Search evidence
Sort evidence
Archive evidence
Create evidence download URL
Link evidence to compliance item
Unlink evidence from compliance item
List evidence links for compliance item
```

Main endpoints:

```http
POST   /api/v1/organizations/{organizationId}/evidence/upload-url
POST   /api/v1/organizations/{organizationId}/evidence
GET    /api/v1/organizations/{organizationId}/evidence?page=0&size=20
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

Evidence list supports:

```txt
pagination:
  page
  size

filters:
  evidenceType
  sourceType

keyword search:
  q

sorting:
  sortBy
  sortDirection
```

Allowed evidence sort fields:

```txt
createdAt
updatedAt
title
evidenceType
sourceType
```

---

### Evidence File Upload Flow

Implemented presigned upload flow:

```txt
1. Frontend sends file metadata to backend.
2. Backend returns objectKey and uploadUrl.
3. Frontend uploads file directly to object storage.
4. Frontend creates evidence metadata with fileObjectKey.
5. Evidence appears in library.
```

Upload URL request:

```json
{
  "filename": "mfa-screenshot.png",
  "contentType": "image/png",
  "fileSizeBytes": 12345
}
```

Important note:

```txt
Backend expects filename, not fileName.
```

---

### Evidence File Download Flow

Implemented presigned download flow:

```txt
1. Frontend requests download URL for evidence.
2. Backend returns presigned GET URL.
3. Frontend opens download URL.
4. User can view or download file.
```

Endpoint:

```http
POST /api/v1/organizations/{organizationId}/evidence/{evidenceId}/download-url
```

---

### Compliance Tasks

Implemented:

```txt
Create task
List tasks
Task pagination
Task filters
Task keyword search
Task sorting
Task summary
Update task
Delete task
Audit task actions
```

Main endpoints:

```http
POST   /api/v1/organizations/{organizationId}/tasks
GET    /api/v1/organizations/{organizationId}/tasks?page=0&size=20
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

Task list supports:

```txt
pagination:
  page
  size

filters:
  status
  priority
  assigneeUserId
  complianceItemId

keyword search:
  q

sorting:
  sortBy
  sortDirection
```

Allowed task sort fields:

```txt
createdAt
updatedAt
dueDate
priority
status
title
```

---

### Audit Trail

Implemented:

```txt
Create audit events for important actions
List audit events
Audit pagination
Audit filters
Audit keyword search
Audit sorting
Recent audit review in frontend
```

Main endpoint:

```http
GET /api/v1/organizations/{organizationId}/audit-events?page=0&size=20
```

Audit list supports:

```txt
pagination:
  page
  size

filters:
  action
  resourceType

keyword search:
  q

sorting:
  sortBy
  sortDirection
```

Allowed audit sort fields:

```txt
createdAt
action
resourceType
actorEmail
```

Audit actions include:

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

Audit resource types:

```txt
COMPLIANCE_FRAMEWORK
COMPLIANCE_ITEM
EVIDENCE_DOCUMENT
EVIDENCE_LINK
COMPLIANCE_TASK
```

---

### API Quality

Implemented:

```txt
Standard error response
Request ID generation
X-Request-Id response header
Request ID propagation
Validation error field violations
Invalid enum query handling
Unsupported sort field handling
CORS configuration
Public health endpoints
Swagger/OpenAPI support
```

Error response shape:

```json
{
  "timestamp": "2026-05-28T...",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/example",
  "requestId": "request-id",
  "fieldViolations": []
}
```

---

### Backend Infrastructure

Implemented:

```txt
Dockerfile
docker-compose local services
docker-compose production-like setup
PostgreSQL service
MinIO service
Actuator health/info
GitHub Actions CI
README documentation
FE API contract v0.9
```

---

## Frontend Completed Scope

### Frontend Foundation

Implemented:

```txt
Next.js App Router
TypeScript
Tailwind CSS
shadcn/ui
TanStack Query
Cookie auth storage
API client
Refresh token retry
Request ID handling
Global providers
B2B compliance SaaS theme
```

---

### Authentication UI

Implemented:

```txt
Landing page
Login page
Register page
Logout flow
Auth guard
Protected route handling
Hydration-safe protected layout
Cookie-based session check
```

Auth cookie names:

```txt
complipilot_access_token
complipilot_refresh_token
```

---

### SaaS App Shell

Implemented:

```txt
Fixed sidebar
Fixed topbar
Scrollable main content only
Workspace selector
Active organization cookie
Navigation links
User info in topbar
Logout button
```

Protected routes:

```txt
/dashboard
/compliance
/evidence
/tasks
/audit
```

---

### Dashboard

Implemented:

```txt
Compliance readiness
Task summary
Due soon controls
Overdue controls
Open tasks
Compliance status breakdown
Recent audit activity
Seed demo workspace button
```

---

### Compliance Page

Implemented:

```txt
Compliance summary cards
Compliance control list
Status badges
Status update
Notes update
Seed demo workspace if empty
Management permission handling
```

---

### Evidence Page

Implemented:

```txt
Evidence list
Pagination
Filter by evidence type
Filter by source type
Search q
Sort by supported fields
Create URL evidence
Upload file evidence
Download file evidence
Archive evidence
Link evidence to compliance item
Unlink evidence from compliance item
```

---

### Tasks Page

Implemented frontend support for backend task capabilities:

```txt
Task list
Pagination
Filters
Search
Sorting
Task summary
Create task
Update task
Delete task
```

---

### Audit Page

Implemented frontend support for backend audit capabilities:

```txt
Audit list
Pagination
Filter by action
Filter by resource type
Search
Sorting
Audit event review
```

---

## End-to-End Demo Flow

Recommended demo flow:

```txt
1. Start backend services.
2. Start frontend.
3. Register a new user.
4. Login.
5. Select active organization.
6. Seed demo workspace.
7. Open dashboard and review summary.
8. Open compliance page.
9. Update control status and notes.
10. Open evidence page.
11. Create URL evidence.
12. Upload file evidence.
13. Download uploaded file evidence.
14. Link evidence to a compliance control.
15. Unlink evidence from a compliance control.
16. Open tasks page.
17. Create / update / delete task.
18. Open audit page.
19. Verify all actions are recorded.
20. Logout.
21. Login again.
22. Refresh protected pages and verify auth flow.
```

---

## Manual Verification Checklist

Backend:

```txt
mvn clean test passes
Docker compose services are healthy
Backend starts successfully
Health endpoint works
Swagger works locally
```

Frontend:

```txt
npm run lint passes
npm run build passes
Login works
Refresh protected route works
Logout works
No hydration mismatch
No expired-token login issue
Upload works
Download works
Archive works
Audit records appear
```

Full local commands:

```powershell
cd D:\GitHub\complipilot-backend
docker compose up -d
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

```powershell
cd D:\GitHub\complipilot-frontend
npm run lint
npm run build
npm run dev
```

---

## Known MVP Limitations

Current known limitations:

```txt
Frontend cookies are client-readable through js-cookie.
Production should move auth to HttpOnly Secure cookies.
No email invitation flow yet.
No advanced organization member management UI yet.
No PDF compliance report export yet.
No AI evidence extraction yet.
No notification/reminder system yet.
No advanced audit metadata viewer yet.
No production object storage provider finalized yet.
```

---

## Recommended Next Improvements

Priority order:

```txt
K1 — Production deployment
K2 — HttpOnly cookie auth layer
K3 — Organization member invitation flow
K4 — Compliance report PDF export
K5 — Evidence detail drawer
K6 — Audit event metadata viewer
K7 — Task reminders / overdue notifications
K8 — AI evidence extraction
K9 — Compliance assistant chatbot
K10 — Advanced role management UI
```

---

## MVP Release Status

Status:

```txt
MVP feature scope completed.
Backend and frontend are integrated.
System is ready for local demo and deployment preparation.
```

Recommended next action:

```txt
Run full verification.
Commit backend and frontend.
Prepare deployment.
Deploy backend first.
Deploy frontend after backend URL is stable.
```
